package org.kie.server.remote.graphql.jbpm.constants;

public class GraphQLConstants {

    private GraphQLConstants() {
        // non-instantiable class
    }

    public static class Fields {
        public static final String ID = "id";
        public static final String BATCH_SIZE = "batchSize";
        public static final String CONTAINER_ID = "containerId";
        public static final String PROCESS_INSTANCE_ID = "processInstanceId";
        public static final String DEFINITION_ID = "definitionId";
        public static final String TASK_INPUT_MAPPINGS = "taskInputMappings";
        public static final String TASK_OUTPUT_MAPPINGS = "taskOutputMappings";
        public static final String VARIABLES = "variables";
    }

    public static class Values {
        public static final int DEFAULT_ALL_PROCESS_DEFINITION_BATCH_SIZE = 100;
        public static final int DEFAULT_ALL_PROCESS_INSTANCE_BATCH_SIZE = 100;
        public static final int DEFAULT_ALL_TASKS_INSTANCE_BATCH_SIZE = 100;
    }

    public static class Arguments {
        public static final String CORRELATION_KEY = "correlationKey";
        public static final String PROCESS_VARIABLES = "processVariables";
    }

}
