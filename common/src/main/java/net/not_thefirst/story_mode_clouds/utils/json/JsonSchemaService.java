package net.not_thefirst.story_mode_clouds.utils.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.networknt.schema.*;

import net.not_thefirst.story_mode_clouds.mesh_builder_api.types.MeshTypeData.ConfigEntry;

import java.util.Set;

public class JsonSchemaService {
    private static final ObjectMapper jacksonMapper = new ObjectMapper();
    private static final Gson gson = 
        new GsonBuilder()
            .registerTypeAdapter(ConfigEntry.class, new ConfigEntryAdapter())
            .create();
    
    private final JsonSchema schema;

    public JsonSchemaService(String schemaContent) throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonNode schemaNode = jacksonMapper.readTree(schemaContent);
        this.schema = factory.getSchema(schemaNode);
    }

    public <T> T validateAndProcess(String rawJson, Class<T> targetClass) throws Exception {
        JsonNode dataNode = jacksonMapper.readTree(rawJson);
        Set<ValidationMessage> errors = schema.validate(dataNode);
        
        if (!errors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("JSON Schema validation failed:\n");
            for (ValidationMessage error : errors) {
                errorMessage.append(" - ").append(error.getMessage()).append("\n");
            }
            throw new IllegalArgumentException(errorMessage.toString());
        }
        
        return gson.fromJson(rawJson, targetClass);
    }
}
