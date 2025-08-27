# Deployment Guide

## Prerequisites

1. **AWS CLI** - [Install AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
2. **SAM CLI** - [Install SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
3. **Python 3.9+** - [Install Python](https://www.python.org/downloads/)
4. **Docker** - [Install Docker](https://docs.docker.com/get-docker/)

## Configuration

1. Configure AWS credentials:

   ```bash
   aws configure
   ```

2. Set your default region (e.g., us-east-1):
   ```bash
   aws configure set region us-east-1
   ```

## Build and Deploy

1. **Build the application**:

   ```bash
   sam build --use-container
   ```

2. **Deploy the application** (first time):

   ```bash
   sam deploy --guided
   ```

   During guided deployment, you'll be prompted for:

   - **Stack Name**: e.g., `task-handler-stack`
   - **AWS Region**: e.g., `us-east-1`
   - **S3BucketName**: e.g., `my-task-summaries-bucket-unique-name`
   - **Confirm changes before deploy**: `Y`
   - **Allow SAM CLI IAM role creation**: `Y`
   - **Save arguments to samconfig.toml**: `Y`

3. **Subsequent deployments**:
   ```bash
   sam build --use-container
   sam deploy
   ```

## Testing

### Local Testing

1. **Test with EventBridge event**:

   ```bash
   sam local invoke TaskHandlerFunction -e events/eventbridge-event.json
   ```

2. **Run unit tests**:
   ```bash
   cd tests
   pip install -r requirements.txt
   python -m pytest unit/ -v
   ```

### Integration Testing

After deployment, you can send test events to EventBridge:

```bash
aws events put-events --entries '[
  {
    "Source": "task.manager",
    "DetailType": "Task Updated",
    "Detail": "{\"id\": 2, \"title\": \"Test Task\", \"description\": \"Test Description\", \"status\": \"COMPLETED\", \"dueDate\": \"2025-09-05T17:00:00\", \"createdAt\": \"2025-08-26T23:30:43.084639\", \"updatedAt\": \"2025-08-27T00:47:55.599711\"}"
  }
]'
```

## Monitoring

1. **CloudWatch Logs**: Check function logs in AWS Console
2. **S3 Bucket**: Verify task summaries are stored in the S3 bucket
3. **CloudWatch Metrics**: Monitor Lambda function metrics

## Troubleshooting

### Common Issues

1. **S3 Bucket already exists**:

   - Change the S3BucketName parameter to a unique name
   - Or delete the existing bucket if it's safe to do so

2. **Permission Issues**:

   - Ensure your AWS user has permissions to create IAM roles, Lambda functions, S3 buckets, and EventBridge rules

3. **Docker Issues**:
   - Ensure Docker is running before using `sam build --use-container`

### Useful Commands

- **View stack outputs**:

  ```bash
  aws cloudformation describe-stacks --stack-name <your-stack-name> --query 'Stacks[0].Outputs'
  ```

- **Delete the stack**:

  ```bash
  sam delete
  ```

- **View logs**:
  ```bash
  sam logs -n TaskHandlerFunction --stack-name <your-stack-name> --tail
  ```
