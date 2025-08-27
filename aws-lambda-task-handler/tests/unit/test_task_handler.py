import json
import pytest
from unittest.mock import patch, MagicMock
import os
import sys

# Add the project root to Python path so we can import hello_world
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..'))

from hello_world import app


@pytest.fixture()
def eventbridge_event():
    """ Generates EventBridge Event with task data"""
    return {
        "version": "0",
        "id": "12345678-1234-1234-1234-123456789012",
        "detail-type": "Task Updated",
        "source": "task.manager",
        "account": "123456789012",
        "time": "2025-08-27T00:47:55Z",
        "region": "us-east-1",
        "detail": {
            "id": 2,
            "title": "Test Task",
            "description": "Test task description",
            "status": "COMPLETED",
            "dueDate": "2025-09-05T17:00:00",
            "createdAt": "2025-08-26T23:30:43.084639",
            "updatedAt": "2025-08-27T00:47:55.599711"
        }
    }


@pytest.fixture()
def lambda_context():
    """ Generates Lambda Context"""
    class MockLambdaContext:
        def __init__(self):
            self.function_name = "test-task-handler"
            self.function_version = "$LATEST"
            self.invoked_function_arn = "arn:aws:lambda:us-east-1:123456789012:function:test-task-handler"
            self.memory_limit_in_mb = 128
            self.remaining_time_in_millis = 30000
    
    return MockLambdaContext()


@patch.dict(os.environ, {'S3_BUCKET_NAME': 'test-bucket'})
@patch('hello_world.app.s3_client')
def test_lambda_handler_success(mock_s3_client, eventbridge_event, lambda_context):
    """ Test successful processing of EventBridge task event """
    
    # Mock S3 client
    mock_s3_client.put_object.return_value = {'ETag': '"test-etag"'}
    
    # Call the lambda handler
    response = app.lambda_handler(eventbridge_event, lambda_context)
    
    # Verify response
    assert response["statusCode"] == 200
    response_body = json.loads(response["body"])
    assert response_body["message"] == "Task processed successfully"
    assert response_body["taskId"] == 2
    assert "s3Location" in response_body
    
    # Verify S3 put_object was called
    mock_s3_client.put_object.assert_called_once()
    call_args = mock_s3_client.put_object.call_args
    assert call_args[1]["Bucket"] == "test-bucket"
    assert "task-summaries/2/" in call_args[1]["Key"]
    assert call_args[1]["ContentType"] == "application/json"


@patch.dict(os.environ, {'S3_BUCKET_NAME': 'test-bucket'})
def test_lambda_handler_missing_detail(lambda_context):
    """ Test handling of EventBridge event without detail """
    
    event_without_detail = {
        "version": "0",
        "id": "12345678-1234-1234-1234-123456789012",
        "detail-type": "Task Updated",
        "source": "task.manager"
        # Missing detail field
    }
    
    response = app.lambda_handler(event_without_detail, lambda_context)
    
    assert response["statusCode"] == 500
    response_body = json.loads(response["body"])
    assert response_body["message"] == "Error processing task"


@patch.dict(os.environ, {'S3_BUCKET_NAME': 'test-bucket'})
@patch('hello_world.app.s3_client')
def test_lambda_handler_s3_error(mock_s3_client, eventbridge_event, lambda_context):
    """ Test handling of S3 error """
    
    # Mock S3 client to raise exception
    mock_s3_client.put_object.side_effect = Exception("S3 error")
    
    response = app.lambda_handler(eventbridge_event, lambda_context)
    
    assert response["statusCode"] == 500
    response_body = json.loads(response["body"])
    assert response_body["message"] == "Error processing task"


def test_task_summary_structure(eventbridge_event, lambda_context):
    """ Test that task summary contains all expected fields """
    
    with patch.dict(os.environ, {'S3_BUCKET_NAME': 'test-bucket'}):
        with patch('hello_world.app.s3_client') as mock_s3_client:
            mock_s3_client.put_object.return_value = {'ETag': '"test-etag"'}
            
            app.lambda_handler(eventbridge_event, lambda_context)
            
            # Get the task summary that was written to S3
            call_args = mock_s3_client.put_object.call_args
            task_summary_json = call_args[1]["Body"]
            task_summary = json.loads(task_summary_json)
            
            # Verify all expected fields are present
            expected_fields = ["id", "title", "description", "status", "dueDate", 
                             "createdAt", "updatedAt"]
            for field in expected_fields:
                assert field in task_summary
            
            # Verify no unexpected fields are present
            assert len(task_summary) == len(expected_fields)


def test_different_task_statuses(lambda_context):
    """ Test that we can handle different task statuses (though EventBridge filters for COMPLETED) """
    
    # Test data for different statuses
    statuses = ["COMPLETED", "PENDING", "IN_PROGRESS", "CANCELLED"]
    
    for status in statuses:
        with patch.dict(os.environ, {'S3_BUCKET_NAME': 'test-bucket'}):
            with patch('hello_world.app.s3_client') as mock_s3_client:
                mock_s3_client.put_object.return_value = {'ETag': '"test-etag"'}
                
                # Create event with different status
                event = {
                    "version": "0",
                    "id": f"test-event-{status.lower()}",
                    "detail-type": "Task Updated",
                    "source": "task.manager",
                    "detail": {
                        "id": 999,
                        "title": f"Test Task - {status}",
                        "description": f"Task with {status} status",
                        "status": status,
                        "dueDate": "2025-09-05T17:00:00",
                        "createdAt": "2025-08-26T23:30:43.084639",
                        "updatedAt": "2025-08-27T00:47:55.599711"
                    }
                }
                
                # Call the lambda handler
                response = app.lambda_handler(event, lambda_context)
                
                # All statuses should be processed successfully by Lambda
                # (EventBridge filtering happens before Lambda is invoked)
                assert response["statusCode"] == 200
                response_body = json.loads(response["body"])
                assert response_body["message"] == "Task processed successfully"
                assert response_body["taskId"] == 999
                
                # Verify the status is preserved in S3 storage
                call_args = mock_s3_client.put_object.call_args
                task_summary_json = call_args[1]["Body"]
                task_summary = json.loads(task_summary_json)
                assert task_summary["status"] == status
