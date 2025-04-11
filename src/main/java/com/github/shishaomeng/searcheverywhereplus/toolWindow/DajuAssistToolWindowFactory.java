// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.github.shishaomeng.searcheverywhereplus.toolWindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandler;
import org.cef.network.CefRequest;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class DajuAssistToolWindowFactory implements ToolWindowFactory, DumbAware {


  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    // 检查 JCEF 是否可用
    if (!JBCefApp.isSupported()) {
      JOptionPane.showMessageDialog(null, "JCEF is not supported on this platform.");
      return;
    }


    // 创建一个面板来容纳浏览器组件和刷新按钮
    JPanel panel = new JPanel(new BorderLayout());
    // 创建一个 JBCefBrowser 实例
    JBCefBrowser browser = new JBCefBrowser();
    // 将浏览器组件添加到面板中
    panel.add(browser.getComponent(), BorderLayout.CENTER);

    // 创建刷新按钮
    JButton refreshButton = new JButton("刷新");
    refreshButton.setVisible(false);

    // 刷新按钮的点击事件
    refreshButton.addActionListener(e -> {
      refreshButton.setVisible(false);
      browser.loadURL("https://agent.dingtalk.com/copilot?code=dSpQ69035G");
    });

    // 将刷新按钮添加到面板的底部
    panel.add(refreshButton, BorderLayout.SOUTH);

    // 增加页面加载处理器
    addPageLoadHandler(browser, refreshButton);

    // 加载指定的 URL
    browser.loadURL("https://agent.dingtalk.com/copilot?code=dSpQ69035G");

    // 将面板添加到工具窗口的内容中
    Content content = ContentFactory.getInstance().createContent(panel, "智能问答", false);
    // 将面板添加到工具窗口的内容中
    Content content2 = ContentFactory.getInstance().createContent(panel, "智能运维", false);
    toolWindow.getContentManager().addContent(content);
    toolWindow.getContentManager().addContent(content2);

  }

  /**
   * 增加页面加载处理器
   *
   * @param browser
   * @param refreshButton
   */
  private void addPageLoadHandler(JBCefBrowser browser, JButton refreshButton) {
    // 监听浏览器的加载状态
    browser.getJBCefClient().addLoadHandler(new CefLoadHandler() {
      @Override
      public void onLoadingStateChange(CefBrowser cefBrowser, boolean isLoading, boolean canGoBack, boolean canGoForward) {
        // 页面加载完成时检查是否加载失败
        if (!isLoading) {
          String url = cefBrowser.getURL();
          if (url == null || url.isEmpty() || url.equals("about:blank")) {
            // 如果 URL 为空或加载失败，显示刷新按钮
            refreshButton.setVisible(true);
          }
        }
      }
      @Override
      public void onLoadStart(CefBrowser cefBrowser, CefFrame cefFrame, CefRequest.TransitionType transitionType) {}
      @Override
      public void onLoadEnd(CefBrowser cefBrowser, CefFrame cefFrame, int i) {}

      @Override
      public void onLoadError(CefBrowser cefBrowser, CefFrame cefFrame, ErrorCode errorCode, String s, String s1) {
        showErrorPage(browser);
        refreshButton.setVisible(true);
      }
    }, browser.getCefBrowser());
  }

  /**
   * 显示错误提示页面
   *
   * @param browser JBCefBrowser 实例
   */
  private void showErrorPage(JBCefBrowser browser) {
    String html;
    InputStream stream = getClass().getResourceAsStream("/daju_load_error_page.html");

    if (stream == null) {
      html = "daju_load_error_page not found";
    }else {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          content.append(line).append("\n");
        }
        html = content.toString();
      } catch (IOException e) {
        e.printStackTrace();
        html = "daju_load_error_page load error";
      }
    }

    browser.loadHTML(html);
  }
}
