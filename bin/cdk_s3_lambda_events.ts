#!/usr/bin/env node
import * as cdk from 'aws-cdk-lib';
import { CdkS3LambdaEventsStack } from '../lib/cdk_s3_lambda_events-stack';

const app = new cdk.App();
new CdkS3LambdaEventsStack(app, 'CdkS3LambdaEventsStack');
