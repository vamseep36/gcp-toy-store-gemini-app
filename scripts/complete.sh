export PROJECT_NUMBER=$(gcloud projects describe "$GOOGLE_CLOUD_PROJECT" --format="value(projectNumber)")

# Add IAM policy binding
gcloud projects add-iam-policy-binding "$GOOGLE_CLOUD_PROJECT" \
    --member="serviceAccount:service-$PROJECT_NUMBER@service-networking.iam.gserviceaccount.com" \
    --role="roles/servicenetworking.serviceAgent"

gcloud projects add-iam-policy-binding $GOOGLE_CLOUD_PROJECT \
  --member="serviceAccount:service-$PROJECT_NUMBER@gcp-sa-alloydb.iam.gserviceaccount.com" \
  --role="roles/aiplatform.user"

gcloud alloydb clusters create vector-cluster \
    --region=us-central1 \
    --network=default \
    --database-version=POSTGRES_15 \
    --password=alloydb


gcloud compute networks peerings create default-peering \
    --network=default \
    --peer-project=$GOOGLE_CLOUD_PROJECT \
    --peer-network=servicenetworking \
    --export-custom-routes \
    --import-custom-routes

gcloud compute addresses create google-managed-services \
    --global \
    --purpose=VPC_PEERING \
    --prefix-length=16 \
    --network=default


gcloud services vpc-peerings connect \
    --service=servicenetworking.googleapis.com \
    --network=default \
    --ranges=google-managed-services

    
gcloud alloydb instances create vector-instance \
    --cluster=vector-cluster \
    --region=us-central1 \
    --instance-type=PRIMARY \
    --cpu-count=2

gcloud compute instances create bastion-host \
    --zone=us-central1-f \
    --metadata startup-script='#! /bin/bash
sudo apt update 
sudo apt install -y postgresql-client' \
    --machine-type=f1-micro \
    --image-family=debian-11 \
    --image-project=debian-cloud \
    --network=default

gcloud compute ssh bastion-host --zone=us-central1-f  -q 
export PRIMARY_IP=$(gcloud alloydb instances describe vector-instance --cluster vector-cluster  --region=us-central1  --format="value(ipAddresses.ipAddress)")
psql -h $PRIMARY_IP -U postgres -d postgres

CREATE EXTENSION IF NOT EXISTS google_ml_integration CASCADE;
CREATE EXTENSION IF NOT EXISTS vector;
select extname, extversion from pg_extension;
CREATE TABLE toys ( id VARCHAR(25), name VARCHAR(25), description VARCHAR(20000), quantity INT, price FLOAT, image_url VARCHAR(200), text_embeddings vector(768)) ;

UPDATE toys set text_embeddings = embedding( 'text-embedding-005', description);
