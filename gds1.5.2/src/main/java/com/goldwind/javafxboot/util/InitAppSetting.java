package com.goldwind.javafxboot.util;

import com.goldwind.javafxboot.model.*;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xujianhua
 * @date 2024-04-22
 * @apiNote
 */
@Slf4j
public class InitAppSetting {
    private InitAppSetting() {
        throw new IllegalStateException("Utility class");
    }

    private static final String DEBUG_SETTING_PATH = File.separator + "src" +
            File.separator + "main" +
            File.separator + "resources" +
            File.separator + "config" +
            File.separator + "setting.xml";
    private static final String PUBLISH_SETTING_PATH = File.separator + "config" +
            File.separator + "setting.xml";
    private static final String DEBUG_PROTOCOL_PATH = File.separator + "src" +
            File.separator + "main" +
            File.separator + "resources" +
            File.separator + "protocol" +
            File.separator;
    private static final String PUBLISH_PROTOCOL_PATH = File.separator + "protocol" +
            File.separator;
    private static final String USER_DIR = "user.dir";
    private static final AppSetting appSetting = new AppSetting();

    public static AppSetting getSetting() {
        return appSetting;
    }

    public static Boolean init() {
        boolean initSettingState = initSetting();
        if (!initSettingState) {
            return false;
        }
        return initProtocol();
    }

    /**
     * 初始化读取文档
     */
    private static Document initDocument(File debugSettingFile, File publishSettingFile) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            if (debugSettingFile.exists()) {
                // 调试模式
                log.info("Init file named [{}] on debug model!", debugSettingFile.getName());
                return dBuilder.parse(debugSettingFile);
            }
            if (publishSettingFile.exists()) {
                // 部署模式
                log.info("Init file named [{}] on publish model!", publishSettingFile.getName());
                return dBuilder.parse(publishSettingFile);
            }
        } catch (Exception e) {
            log.error("error to init document: ", e);
        }
        return null;
    }

    /**
     * 初始化配置文档（setting)
     */
    private static Boolean initSetting() {
        try {
            // 读取初始化配置信息(setting.xml)
            File debugSettingFile = new File(System.getProperty(USER_DIR) + DEBUG_SETTING_PATH);
            File publishSettingFile = new File(System.getProperty(USER_DIR) + PUBLISH_SETTING_PATH);
            Document doc = initDocument(debugSettingFile, publishSettingFile);
            if (doc == null) {
                // 未找到初始化配置文件
                log.warn("Failed to get setting document!");
                return false;
            }
            doc.getDocumentElement().normalize();

            // 获取environment配置信息
            NodeList environmentList = doc.getElementsByTagName("environment");
            for (int i = 0; i < environmentList.getLength(); i++) {
                Element property = (Element) environmentList.item(i);
                SettingEnvironment settingEnvironment = new SettingEnvironment();
                settingEnvironment.setLanguage(property.getAttribute("language"));
                settingEnvironment.setProtocol(property.getAttribute("protocol"));
                appSetting.setSettingEnvironment(settingEnvironment);
            }

            // 获取server配置信息
            NodeList serverList = doc.getElementsByTagName("server");
            for (int i = 0; i < serverList.getLength(); i++) {
                Element property = (Element) serverList.item(i);
                SettingServer settingServer = new SettingServer();
                settingServer.setIp(property.getAttribute("ip"));
                settingServer.setSlaveId(Integer.parseInt(property.getAttribute("slave")));
                settingServer.setPort(Integer.parseInt(property.getAttribute("port")));
                settingServer.setCycle(Integer.parseInt(property.getAttribute("cycle")));
                appSetting.setSettingServer(settingServer);
            }

            // 获取界面显示分栏配置信息
            List<SettingShow> settingShowList = new ArrayList<>();
            NodeList showList = doc.getElementsByTagName("show");
            for (int i = 0; i < showList.getLength(); i++) {
                Element property = (Element) showList.item(i);
                SettingShow settingShow = new SettingShow();
                settingShow.setIec(property.getAttribute("iec"));
                settingShow.setIndex(Integer.parseInt(property.getAttribute("index")));
                settingShow.setColumn(Integer.parseInt(property.getAttribute("column")));
                settingShowList.add(settingShow);
            }
            appSetting.setSettingShowList(settingShowList);
            return true;
        } catch (Exception e) {
            log.error("Failed to init setting", e);
            return false;
        }
    }

    /**
     * 初始化协议文档（protocol)
     */
    private static Boolean initProtocol() {
        try {
            // 根据配置协议，获取采集分组信息
            File debugProtocolFile = new File(System.getProperty(USER_DIR) + DEBUG_PROTOCOL_PATH + appSetting.getSettingEnvironment().getProtocol() + ".xml");
            File publishProtocolFile = new File(System.getProperty(USER_DIR) + PUBLISH_PROTOCOL_PATH + appSetting.getSettingEnvironment().getProtocol() + ".xml");
            Document protocolDoc = initDocument(debugProtocolFile, publishProtocolFile);
            if (protocolDoc == null) {
                // 未找到初始化配置文件
                log.warn("Failed to get protocol document!");
                return false;
            }

            protocolDoc.getDocumentElement().normalize();
            // 获取采集分组
            List<SettingGroup> settingGroupList = new ArrayList<>();
            NodeList groupList = protocolDoc.getElementsByTagName("group");
            for (int i = 0; i < groupList.getLength(); i++) {
                Element group = (Element) groupList.item(i);
                SettingGroup settingGroup = new SettingGroup();
                // 当前组序号
                settingGroup.setIndex(Integer.parseInt(group.getAttribute("index")));
                // 当前组采集起始位
                settingGroup.setStart(Integer.parseInt(group.getAttribute("start")));
                // 当前组采集结束位
                settingGroup.setEnd(Integer.parseInt(group.getAttribute("end")));
                // 初始化地址信息
                List<SettingLocation> settingLocationList = new ArrayList<>();
                // 获取当前组采集地址配置信息
                NodeList locationsList = group.getElementsByTagName("location");
                for (int k = 0; k < locationsList.getLength(); k++) {
                    Element adr = (Element) locationsList.item(k);
                    SettingLocation settingLocation = new SettingLocation();
                    settingLocation.setAddress(Integer.parseInt(adr.getAttribute("address")));
                    settingLocation.setUnit(adr.getAttribute("unit"));
                    settingLocation.setConvert(Integer.parseInt(adr.getAttribute("convert")));
                    if(!adr.getAttribute("type").toString().equals("")){
                        settingLocation.setType(Integer.parseInt(adr.getAttribute("type")));
                    }
                    settingLocation.setMultiple(Integer.parseInt(adr.getAttribute("multiple")));
                    settingLocation.setShow(Integer.parseInt(adr.getAttribute("show")));
                    settingLocation.setIec(adr.getAttribute("iec"));
                    settingLocation.setFnc_lab(Integer.parseInt(adr.getAttribute("fnc_lab")));


                    // 获取当前采集地址枚举类型配置信息
                    List<SettingEnum> enumsList = new ArrayList<>();
                    NodeList enumNodeList = adr.getElementsByTagName("enum");
                    for (int m = 0; m < enumNodeList.getLength(); m++) {
                        Element enumData = (Element) enumNodeList.item(m);
                        SettingEnum settingEnum = new SettingEnum();
                        settingEnum.setState(enumData.getAttribute("state"));
                        settingEnum.setIec(enumData.getAttribute("iec"));
                        enumsList.add(settingEnum);
                    }
                    settingLocation.setSettingEnumList(enumsList);
                    settingLocationList.add(settingLocation);
                }
                settingGroup.setSettingLocationList(settingLocationList);
                settingGroupList.add(settingGroup);
            }
            appSetting.setSettingGroupList(settingGroupList);
            return true;
        } catch (Exception e) {
            log.error("Failed to init protocol", e);
            return false;
        }
    }
}
