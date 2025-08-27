import json
import boto3
import logging
from datetime import datetime
import os

# Configure logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize S3 client
s3_client = boto3.client('s3')

def lambda_handler(event, context):
    """Lambda function to process EventBridge task events

    Parameters
    ----------
    event: dict, required
        EventBridge event containing task object in detail field

    context: object, required
        Lambda Context runtime methods and attributes

    Returns
    ------
    dict: Response indicating success or failure
    """

    try:
        # Log the incoming event
        logger.info(f"Received EventBridge event: {json.dumps(event)}")
        
        # Extract task object from EventBridge event
        # EventBridge events have the actual data in the 'detail' field
        task_data = event.get('detail', {})
        
        if not task_data:
            logger.error("No task data found in event detail")
            raise ValueError("Missing task data in event detail")
        
        # Log task details
        task_status = task_data.get('status')
        logger.info(f"Processing task: ID={task_data.get('id')}, Title={task_data.get('title')}, Status={task_status}")
        
        # EventBridge rule filters for COMPLETED status, expecting only COMPLETED tasks therefore
        if task_status != 'COMPLETED':
            logger.warning(f"Invalid task status received '{task_status}'")
        
        # Create task summary for S3 storage
        task_summary = {
            "id": task_data.get("id"),
            "title": task_data.get("title"),
            "description": task_data.get("description"),
            "status": task_data.get("status"),
            "dueDate": task_data.get("dueDate"),
            "createdAt": task_data.get("createdAt"),
            "updatedAt": task_data.get("updatedAt")
        }
    
        # Store in S3
        bucket_name = os.environ.get('S3_BUCKET_NAME', 'task-details-bucket')
        s3_key = f"task-summaries/{datetime.utcnow().strftime('%Y/%m/%d')}/task-{task_data.get('id')}-{int(datetime.utcnow().timestamp())}.json"
        
        # Upload to S3
        s3_client.put_object(
            Bucket=bucket_name,
            Key=s3_key,
            Body=json.dumps(task_summary, indent=2),
            ContentType='application/json'
        )
        
        logger.info(f"Task summary stored in S3: s3://{bucket_name}/{s3_key}")
        
        return {
            "statusCode": 200,
            "body": json.dumps({
                "message": "Task processed successfully",
                "taskId": task_data.get("id"),
                "s3Location": f"s3://{bucket_name}/{s3_key}"
            })
        }
        
    except Exception as e:
        logger.error(f"Error processing task: {str(e)}")
        return {
            "statusCode": 500,
            "body": json.dumps({
                "message": "Error processing task",
                "error": str(e)
            })
        }
