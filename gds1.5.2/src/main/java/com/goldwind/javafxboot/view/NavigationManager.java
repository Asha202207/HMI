package com.goldwind.javafxboot.view;

import javafx.beans.property.*;
import javafx.scene.Node;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 导航管理器 - 管理界面导航和页面切换
 * 
 * @author HMI Team
 * @date 2026-01-13
 */
@Slf4j
public class NavigationManager {
    
    private static volatile NavigationManager instance;
    
    /**
     * 导航页面枚举
     */
    public enum Page {
        MAIN("main", "主界面"),
        COMPONENT("component", "部件"),
        FAULT("fault", "故障"),
        LOGIN("login", "登录"),
        CONTROL("control", "控制");
        
        private final String code;
        private final String displayName;
        
        Page(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 当前页面
     */
    private final ObjectProperty<Page> currentPage = new SimpleObjectProperty<>(Page.MAIN);
    
    /**
     * 页面视图缓存
     */
    private final Map<Page, Node> pageCache = new HashMap<>();
    
    /**
     * 页面创建器
     */
    private final Map<Page, Supplier<Node>> pageCreators = new HashMap<>();
    
    /**
     * 内容区域更新回调
     */
    private Runnable onPageChanged;
    
    private NavigationManager() {}
    
    /**
     * 获取单例实例
     */
    public static NavigationManager getInstance() {
        if (instance == null) {
            synchronized (NavigationManager.class) {
                if (instance == null) {
                    instance = new NavigationManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 注册页面创建器
     */
    public void registerPageCreator(Page page, Supplier<Node> creator) {
        pageCreators.put(page, creator);
    }
    
    /**
     * 导航到指定页面
     */
    public void navigateTo(Page page) {
        if (page == null) {
            return;
        }
        
        Page oldPage = currentPage.get();
        currentPage.set(page);
        
        log.info("Navigate from {} to {}", oldPage, page);
        
        if (onPageChanged != null) {
            onPageChanged.run();
        }
    }
    
    /**
     * 获取当前页面视图
     */
    public Node getCurrentPageView() {
        Page page = currentPage.get();
        
        // 尝试从缓存获取
        Node cachedView = pageCache.get(page);
        if (cachedView != null) {
            return cachedView;
        }
        
        // 创建新视图
        Supplier<Node> creator = pageCreators.get(page);
        if (creator != null) {
            Node view = creator.get();
            pageCache.put(page, view);
            return view;
        }
        
        return null;
    }
    
    /**
     * 清除页面缓存
     */
    public void clearPageCache() {
        pageCache.clear();
    }
    
    /**
     * 清除指定页面缓存
     */
    public void clearPageCache(Page page) {
        pageCache.remove(page);
    }
    
    /**
     * 获取当前页面
     */
    public Page getCurrentPage() {
        return currentPage.get();
    }
    
    /**
     * 获取当前页面属性（用于绑定）
     */
    public ObjectProperty<Page> currentPageProperty() {
        return currentPage;
    }
    
    /**
     * 设置页面变化回调
     */
    public void setOnPageChanged(Runnable callback) {
        this.onPageChanged = callback;
    }
    
    /**
     * 检查是否为当前页面
     */
    public boolean isCurrentPage(Page page) {
        return page == currentPage.get();
    }
}
