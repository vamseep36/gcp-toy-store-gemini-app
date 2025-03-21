gcloud alloydb clusters create vector-cluster \
    --region=us-central1 \
    --network=default \
    --database-version=POSTGRES_15 \
    --password=alloydb

gcloud alloydb instances create vector-instance \
    --cluster=vector-cluster \
    --region=us-central1 \
    --instance-type=PRIMARY \
    --cpu-count=4
