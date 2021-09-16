# Kafka connect in Kubernetes

## Deploy Azure resources using [Terraform](https://www.terraform.io/) (version >= 0.15 should be installed on your system)
```
terraform init
terraform plan -out terraform.plan
terraform apply terraform.plan
....
terraform destroy
```

## Install Confluent Hub Client

You can find the installation manual [here](https://docs.confluent.io/home/connect/confluent-hub/client.html)

## Create a custom docker image

For running the azure connector, you can create your own docker image. Create your azure connector image and build it.
Run commands 'docker build -t avpash43/my-azure-connector12:latest -f connectors/Dockerfile .'  &&  'docker push avpash43/my-azure-connector12:latest'

![Alt text](screenshots/DockerHub.png?raw=true "Title")

## Launch Confluent for Kubernetes

* az login
* az aks get-credentials --name aks-sparkbasic-westeurope  --resource-group rg-sparkbasic-westeurope

### Create a namespace

- Create the namespace to use:

  ```cmd
  kubectl create namespace confluent
  ```

- Set this namespace to default for your Kubernetes context:

  ```cmd
  kubectl config set-context --current --namespace confluent
  ```

### Install Confluent for Kubernetes

- Add the Confluent for Kubernetes Helm repository:

  ```cmd
  helm repo add confluentinc https://packages.confluent.io/helm
  helm repo update
  ```

- Install Confluent for Kubernetes:

  ```cmd
  helm upgrade --install confluent-operator confluentinc/confluent-for-kubernetes
  ```

### Install Confluent Platform

- Install all Confluent Platform components:

  ```cmd
  kubectl apply -f ./confluent-platform.yaml
  ```

- Install a sample producer app and topic:

  ```cmd
  kubectl apply -f ./producer-app-data.yaml
  ```

- Check that everything is deployed:

  ```cmd
  kubectl get pods -o wide 
  ```

Pods:

![Alt text](screenshots/Pods.png?raw=true "Title")

### View Control Center

- Set up port forwarding to Control Center web UI from local machine:

  ```cmd
  kubectl port-forward controlcenter-0 9021:9021
  ```

- Browse to Control Center: [http://localhost:9021](http://localhost:9021)

## Create a kafka topic

- The topic should have at least 3 partitions because the azure blob storage has 3 partitions. Name the new topic: "expedia".

![Alt text](screenshots/NewTopic.png?raw=true "Title")

## Prepare the azure connector configuration

## Upload the connector file through the API

![Alt text](screenshots/UploadConnectorConfig.png?raw=true "Title")

![Alt text](screenshots/ConnectorRunningTask.png?raw=true "Title")

![Alt text](screenshots/ExpediaTopicRecords.png?raw=true "Title")

## Implement you KStream application

- Add necessary code and configuration to [KStream Application Class](src/main/java/com/epam/bd201/KStreamsApplication.java)

- Build KStream application jar
  ```cmd
  $ mvn package
  ```

- Build [KStream Docker Image](Dockerfile) - insert valid Azure image registry here
  ```cmd
  $ docker build -t avpash43.azurecr.io/kstream-app:1.0 .
  ```

- Push KStream image to Container Registry
  ```cmd
  * az acr login -n avpash43
  * $ docker push avpash43.azurecr.io/kstream-app:1.0
  ```

- Run you KStream app container in the K8s kluster alongside with Kafka Connect. Don't forger to update [Kubernetes deployment](kstream-app.yaml)
  with valid registry for your image
  ```cmd
  $ kubectl create -f kstream-app.yaml
  ```

Expdia_ext topic records:

![Alt text](screenshots/ExpediaExtTopic.png?raw=true "Title")

Stream & Table:

* create stream categories_stream (category VARCHAR, hotel_id BIGINT) with (KAFKA_TOPIC='expedia_ext', VALUE_FORMAT='JSON');
* SET 'auto.offset.reset' = 'earliest';
* SET 'cache.max.bytes.buffering' = '0';

![Alt text](screenshots/DataMartSelect.png?raw=true "Title")

![Alt text](screenshots/DataMartCreateTable.png?raw=true "Title")