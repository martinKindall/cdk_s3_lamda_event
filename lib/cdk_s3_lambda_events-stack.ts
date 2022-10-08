import { Duration, lambda_layer_awscli, Stack, StackProps } from 'aws-cdk-lib';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as s3_notif from 'aws-cdk-lib/aws-s3-notifications';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export class CdkS3LambdaEventsStack extends Stack {
  private lambdaF: lambda.Function;

  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    this.initLambda();
    this.initS3();
  }

  private initLambda() {
    this.lambdaF = new lambda.Function(this, "S3Lambda", {
      runtime: lambda.Runtime.JAVA_11,
      memorySize: 1024,
      handler: "com.codigomorsa.app.Pokedex::handleRequest",
      code: lambda.Code.fromAsset("./app/build/libs/app-1.0-SNAPSHOT-all.jar"),
      timeout: Duration.seconds(10)
    });

    const rekognitionModel = "arn:aws:rekognition:us-east-1:371417955885:project/pokedex/version/pokedex.2022-10-02T12.52.44/1664707965078";
    const rekognitionPolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'rekognition:DetectCustomLabels'
      ],
      resources: [rekognitionModel],
    });

    const sesArnSender = "arn:aws:ses:us-east-1:371417955885:identity/codigomorsa.link";
    const sesArnReceiver = "arn:aws:ses:us-east-1:371417955885:identity/mart256@gmail.com";
    const sesPolicy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        'ses:SendEmail'
      ],
      resources: [sesArnSender, sesArnReceiver],
    });

    this.lambdaF.addEnvironment("REKOGNITION_MODEL", rekognitionModel);
    this.lambdaF.addEnvironment("SES_ARN", sesArnSender);
    this.lambdaF.addEnvironment("EMAIL_ADDR", "pokedex@codigomorsa.link");
    this.lambdaF.addEnvironment("TO_ADDR", "mart256@gmail.com");
    this.lambdaF.addToRolePolicy(rekognitionPolicy);
    this.lambdaF.addToRolePolicy(sesPolicy);
  }

  private initS3() {
    const bucket = new s3.Bucket(this, "PokedexBucket", {
      publicReadAccess: true
    });

    const s3Policy = new iam.PolicyStatement({
      effect: iam.Effect.ALLOW,
      actions: [
        's3:GetObject*'
      ],
      resources: [bucket.arnForObjects('*'), bucket.bucketArn],
      principals: [new iam.AnyPrincipal()], 
    });

    bucket.addToResourcePolicy(s3Policy);

    const s3Trigger = new s3_notif.LambdaDestination(this.lambdaF);
    s3Trigger.bind(this, bucket);

    [".jpg", ".jpeg", ".png"].map(suffix => {
      bucket.addObjectCreatedNotification(s3Trigger, {suffix});
    });
  }
}
