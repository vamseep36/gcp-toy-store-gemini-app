package cloudcode.helloworld.web;



import com.google.api.gax.rpc.ApiException;
import com.google.cloud.aiplatform.v1.EndpointName;
import com.google.cloud.aiplatform.v1.PredictResponse;
import com.google.cloud.aiplatform.v1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GenerateToy {
  static String bytesBase64Encoded = "";
  static String bytesBase64EncodedOuput = "";
    public static void main(String[] args) throws IOException {
      String projectId = "";
      String location = "us-central1";
      String prompt = "";
     // generateImage(projectId, location, prompt);
    }
  
    // Generate an image using a text prompt using an Imagen model
    public String generateImage(String projectId, String location, String prompt)
        throws ApiException, IOException {
      final String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
      PredictionServiceSettings predictionServiceSettings =
      PredictionServiceSettings.newBuilder().setEndpoint(endpoint).build();
      String context = "Generate a photo-realistic image of a toy described in the following input text from the user. Make sure you adhere to all the little details and requirements mentioned in the prompt. Ensure that the user is only describing a toy. If it is anything unrelated to a toy, politely decline the request stating that the request is inappropriate for the current context. ";
      
       
        prompt = context + prompt;
      System.out.format("PROMPT: %s\n", prompt);

      // Initialize client that will be used to send requests. This client only needs to be created
      // once, and can be reused for multiple requests.
      try (PredictionServiceClient predictionServiceClient =
          PredictionServiceClient.create(predictionServiceSettings)) {
  
        final EndpointName endpointName =
            EndpointName.ofProjectLocationPublisherModelName(
                projectId, location, "google", "imagen-3.0-generate-001"); //"imagegeneration@006"; imagen-3.0-generate-001
        Map<String, Object> instancesMap = new HashMap<>();
        instancesMap.put("prompt", prompt);

       // instancesMap.put("image_context", imageContextMap);

       Value instances = mapToValue(instancesMap);
  
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("sampleCount", 1);
        paramsMap.put("aspectRatio", "1:1");
        paramsMap.put("safetyFilterLevel", "block_few");
        paramsMap.put("personGeneration", "allow_adult");
        paramsMap.put("guidanceScale", 21);
        
      paramsMap.put("imagenControlScale", 0.95); //Setting imagenControlScale
        Value parameters = mapToValue(paramsMap);
  
        PredictResponse predictResponse =
            predictionServiceClient.predict(
                endpointName, Collections.singletonList(instances), parameters);
  
        for (Value prediction : predictResponse.getPredictionsList()) {
          Map<String, Value> fieldsMap = prediction.getStructValue().getFieldsMap();
          if (fieldsMap.containsKey("bytesBase64Encoded")) {
            bytesBase64EncodedOuput = fieldsMap.get("bytesBase64Encoded").getStringValue();
        }
      }
      return bytesBase64EncodedOuput.toString();
    }
  }

  private static Value mapToValue(Map<String, Object> map) throws InvalidProtocolBufferException {
    Gson gson = new Gson();
    String json = gson.toJson(map);
    Value.Builder builder = Value.newBuilder();
    JsonFormat.parser().merge(json, builder);
    return builder.build();
  }

}
