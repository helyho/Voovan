package org.voovan.test.tools.json;

import org.voovan.tools.json.JSONDecode;
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
public class JSONDecodeTest {
    public static void main(String[] args) {
        String tmp = "//123123\n" +
                "[{\n" +
                "    \"Id\" = 111111111.1111111111111,//12312313\n" +
                "    \"Names\": [\"/dockerfly\"],\n" +
                "    Image: dockerfly,\n" +
                "    \"ImageID\": \"sha256:84325b476deb2efd4e3f21c9cf8b94a6bb\n" +
                "\t\t\t\t\t882c8327d99b9279be693978\n" +
                "\t\t\t\t\t9d811b\",\n" +
                "    \"Command\": \"/bin/sh -c \\\"/root/dockertunnel\\\"\",\n" +
                "    \"Created\": 1484737485,\n" +
                "    \"Ports\": [{\n" +
                "        \"IP\": \"0.0.0.0\",\n" +
                "        \"PrivatePort\": 2735,\n" +
                "        \"PublicPort\": 2735,\n" +
                "        \"Type\": \"tcp\"\n" +
                "    }],\n" +
                "    \"Labels\": {},\n" +
                "    \"State\": \"running\",\n" +
                "    \"Status\": \"Up About an hour\",\n" +
                "    \"HostConfig\": {\n" +
                "        \"NetworkMode\": \"default\"\n" +
                "    },\n" +
                "    \"NetworkSettings\": {\n" +
                "        \"Networks\": {\n" +
                "            \"bridge\": {\n" +
                "                \"IPAMConfig\": null,\n" +
                "                \"Links\": null,\n" +
                "                \"Aliases\": null,\n" +
                "                \"NetworkID\": \"67c78850ea8e08870615c3b0865f5e3dd81dfce94f54c953bfe8c298ffcfbbef\",\n" +
                "                \"EndpointID\": \"f1f7b1ae4ee4b5afc811e7ee59af25f4d1fa8a7ba9e69c9848d0bd0a6bd45d71\",\n" +
                "                \"Gateway\": \"172.17.0.1\",\n" +
                "                \"IPAddress\": \"172.17.0.2\",\n" +
                "                \"IPPrefixLen\": 16,\n" +
                "                \"IPv6Gateway\": \"\",\n" +
                "                \"GlobalIPv6Address\": \"\",\n" +
                "                \"GlobalIPv6PrefixLen\": 0,\n" +
                "                \"MacAddress\": \"02:42:ac:11:00:02\"\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"Mounts\": [{\n" +
                "        \"Type\": \"bind\",\n" +
                "        \"Source\": \"/var/run/docker.sock\",\n" +
                "        \"Destination\": \"/var/run/docker.sock\",\n" +
                "        \"Mode\": \"\",\n" +
                "        \"RW\": true,\n" +
                "        \"Propagation\": \"rprivate\"\n" +
                "    }]\n" +
                "}]";
        Object result = JSONDecode.parse(tmp);
        Logger.simple(result);
    }
}
