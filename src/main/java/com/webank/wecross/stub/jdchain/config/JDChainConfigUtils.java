package com.webank.wecross.stub.jdchain.config;

import com.webank.wecross.exception.Status;
import com.webank.wecross.exception.WeCrossException;
import com.webank.wecross.resource.Path;
import com.webank.wecross.resource.Resource;
import com.webank.wecross.stub.jdchain.JDChainContractResource;
import com.webank.wecross.utils.ConfigUtils;
import com.webank.wecross.utils.WeCrossType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JDChainConfigUtils {

    public static List<JDChainService> getJDChainService(
            String stubPath, List<Map<String, String>> jdChainServiceMaps) throws WeCrossException {

        List<JDChainService> jDChainServiceList = new ArrayList<JDChainService>();

        for (Map<String, String> jdChainServiceMap : jdChainServiceMaps) {
            String privateKey = jdChainServiceMap.get("privateKey");
            if (privateKey == null) {
                String errorMessage =
                        "\"privateKey\" in [[jdServices]] item  not found, please check "
                                + stubPath;
                throw new WeCrossException(Status.FIELD_MISSING, errorMessage);
            }

            String publicKey = jdChainServiceMap.get("publicKey");
            if (publicKey == null) {
                String errorMessage =
                        "\"publicKey\" in [[jdServices]] item  not found, please check " + stubPath;
                throw new WeCrossException(Status.FIELD_MISSING, errorMessage);
            }

            String password = jdChainServiceMap.get("password");
            if (password == null) {
                String errorMessage =
                        "\"password\" in [[jdServices]] item  not found, please check " + stubPath;
                throw new WeCrossException(Status.FIELD_MISSING, errorMessage);
            }

            String connectionsStr = jdChainServiceMap.get("connectionsStr");
            if (connectionsStr == null) {
                String errorMessage =
                        "\"connectionsStr\" in [[jdServices]] item  not found, please check "
                                + stubPath;
                throw new WeCrossException(Status.FIELD_MISSING, errorMessage);
            }

            jDChainServiceList.add(
                    new JDChainService(privateKey, publicKey, password, connectionsStr));
        }
        return jDChainServiceList;
    }

    public static Map<String, Resource> getJdChainResources(
            String prefix, String stubPath, List<Map<String, String>> resources)
            throws WeCrossException {
        if (resources == null) {
            return null;
        }

        Map<String, Resource> jdChainResources = new HashMap<>();

        for (Map<String, String> resource : resources) {
            String type = resource.get("type");
            if (type == null) {
                String errorMessage =
                        "\"type\" in [[resources]] item  not found, please check " + stubPath;
                throw new WeCrossException(Status.FIELD_MISSING, errorMessage);
            }

            //  handle contract resource
            if (type.equalsIgnoreCase(WeCrossType.RESOURCE_TYPE_JDCHAIN_CONTRACT)) {
                String name = resource.get("name");
                if (name == null) {
                    String errorMessage =
                            "\"name\" in [[resources]] item  not found, please check " + stubPath;
                    throw new WeCrossException(Status.FIELD_MISSING, errorMessage);
                }

                if (jdChainResources.keySet().contains(name)) {
                    String errorMessage =
                            name + " in [[resources]] item  is repeated, please check " + stubPath;
                    throw new WeCrossException(Status.REPEATED_KEY, errorMessage);
                }

                String contractAddress = resource.get("contractAddress");
                if (contractAddress == null) {
                    String errorMessage =
                            "\"contractAddress\" in [[resources]] item  not found, please check "
                                    + stubPath;
                    throw new WeCrossException(Status.FIELD_MISSING, errorMessage);
                }

                JDChainContractResource jdChainContractResource = new JDChainContractResource();
                jdChainContractResource.setContractAddress(contractAddress);

                // set path
                String stringPath = prefix + "." + name;
                try {
                    ConfigUtils.checkPath(stringPath);
                    jdChainContractResource.setPath(Path.decode(stringPath));
                } catch (WeCrossException we) {
                    throw we;
                } catch (Exception e) {
                    throw new WeCrossException(Status.INTERNAL_ERROR, e.getMessage());
                }

                jdChainResources.put(name, jdChainContractResource);

            } else if (type.equals("assets")) {
                // To be defined
                continue;
            } else {
                String errorMessage = "Undefined jdchain resource type: " + type;
                throw new WeCrossException(Status.UNEXPECTED_CONFIG, errorMessage);
            }
        }
        return jdChainResources;
    }
}
