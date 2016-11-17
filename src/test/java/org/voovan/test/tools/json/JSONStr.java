package org.voovan.test.tools.json;

import org.voovan.tools.json.JSONPath;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class JSONStr {
    public static String tmpStr = "[\n" +
            "  {\n" +
            "    \"ID\": \"7hyvnnrbp48bzcvolx35hdoqg\",\n" +
            "    \"Version\": {\n" +
            "      \"Index\": 108573\n" +
            "    },\n" +
            "    \"CreatedAt\": \"2016-10-20T13:31:12.026491602Z\",\n" +
            "    \"UpdatedAt\": \"2016-10-20T13:31:12.071133294Z\",\n" +
            "    \"Spec\": {\n" +
            "      \"Name\": \"test_bv_prog\",\n" +
            "      \"TaskTemplate\": {\n" +
            "        \"ContainerSpec\": {\n" +
            "          \"Image\": \"alpine\",\n" +
            "          \"Args\": [\n" +
            "            \"ping\",\n" +
            "            \"127.0.0.1\"\n" +
            "          ],\n" +
            "          \"Mounts\": [\n" +
            "            {\n" +
            "              \"Type\": \"volume\",\n" +
            "              \"Source\": \"v_t1\",\n" +
            "              \"Target\": \"/v_t1\",\n" +
            "              \"ReadOnly\": true\n" +
            "            }\n" +
            "          ],\n" +
            "          \"StopGracePeriod\": 0\n" +
            "        },\n" +
            "        \"Resources\": {\n" +
            "          \"Limits\": {},\n" +
            "          \"Reservations\": {}\n" +
            "        },\n" +
            "        \"RestartPolicy\": {\n" +
            "          \"Condition\": \"on_failure\",\n" +
            "          \"Delay\": 0,\n" +
            "          \"MaxAttempts\": 0\n" +
            "        }\n" +
            "      },\n" +
            "      \"Mode\": {\n" +
            "        \"Replicated\": {\n" +
            "          \"Replicas\": 1\n" +
            "        }\n" +
            "      },\n" +
            "      \"UpdateConfig\": {\n" +
            "        \"Parallelism\": 2,\n" +
            "        \"FailureAction\": \"pause\"\n" +
            "      },\n" +
            "      \"Networks\": [\n" +
            "        {\n" +
            "          \"Target\": \"89x15fcp9x08ncc885xwn0e9e\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"EndpointSpec\": {\n" +
            "        \"Mode\": \"vip\",\n" +
            "        \"Ports\": [\n" +
            "          {\n" +
            "            \"Protocol\": \"tcp\",\n" +
            "            \"TargetPort\": 80,\n" +
            "            \"PublishedPort\": 8080\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"Endpoint\": {\n" +
            "      \"Spec\": {\n" +
            "        \"Mode\": \"vip\",\n" +
            "        \"Ports\": [\n" +
            "          {\n" +
            "            \"Protocol\": \"tcp\",\n" +
            "            \"TargetPort\": 80,\n" +
            "            \"PublishedPort\": 8080\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"Ports\": [\n" +
            "        {\n" +
            "          \"Protocol\": \"tcp\",\n" +
            "          \"TargetPort\": 80,\n" +
            "          \"PublishedPort\": 8080\n" +
            "        }\n" +
            "      ],\n" +
            "      \"VirtualIPs\": [\n" +
            "        {\n" +
            "          \"NetworkID\": \"chhjg6kzl8v9bc7x300v6axfl\",\n" +
            "          \"Addr\": \"10.255.0.2/16\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"NetworkID\": \"89x15fcp9x08ncc885xwn0e9e\",\n" +
            "          \"Addr\": \"10.0.0.2/24\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    \"UpdateStatus\": {\n" +
            "      \"StartedAt\": \"0001-01-01T00:00:00Z\",\n" +
            "      \"CompletedAt\": \"0001-01-01T00:00:00Z\"\n" +
            "    }\n" +
            "  }\n" +
            "]";

    public static void main(String[] args) throws ReflectiveOperationException {
        JSONPath jsonPath = new JSONPath(tmpStr);
        Logger.info(jsonPath.value("/root[0]/Version/Index"));
    }
}
