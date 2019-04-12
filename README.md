# NestedWebViewRecyclerViewGroup

[ ![Download](https://api.bintray.com/packages/hzwsunshine/maven/NestedWebViewRecyclerViewGroup/images/download.svg) ](https://bintray.com/hzwsunshine/maven/NestedWebViewRecyclerViewGroup/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

NestedWebViewRecyclerViewGroup 是 WebView 和 RecyclerView 的嵌套控件，用于经典的上面是 WebView 显示的文章，下面是 RecyclerView 显示的评论相关的文章详情页结构中，可以实现 WebView 和 RecyclerView 的无缝滑动切换，也提供 WebView 区域和 RecyclerView 区域的上下显示切换

![image](https://github.com/HzwSunshine/NestedWebViewRecyclerViewGroup/blob/master/app/example/example.gif)

</br>

##  Gradle

     compile 'com.github.hzw:NestedWebViewRecyclerViewGroup:'DownLoad Versiokn''

     所需依赖：com.android.support:support-v4:xxx
             com.android.support:recyclerview-v7:xxx

</br>

## 使用

**为了使用方便定义了以下一些属性**：
```xml
//WebView和RecyclerView上下切换的时间
<attr name="changeDuration" format="integer" /> 
//scrollbar是否可用
<attr name="scrollbarEnable" format="boolean" /> 
//scrollbar的颜色
<attr name="scrollbarColor" format="color" /> 
//scrollbar的最小高度
<attr name="scrollbarMinHeight" format="dimension" /> 
//scrollbar的宽度
<attr name="scrollbarWidth" format="dimension" /> 
//scrollbar距离屏幕右边的距离
<attr name="scrollbarMarginRight" format="dimension" /> 
```


**xml**
    
```xml
    <com.hzw.nestedviewgroup.NestedWebViewRecyclerViewGroup
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:changeDuration="300"
        app:scrollbarColor="#000000"
        app:scrollbarEnable="true">

        <com.hzw.nestedviewgroup.NestedScrollWebView
            android:layout_width="match_parent"
            android:layout_height="match_parent">
        </com.hzw.nestedviewgroup.NestedScrollWebView>

        <android.support.v7.widget.RecyclerView
            android:layout_width="match_parent"
            android:layout_height="match_parent">
        </android.support.v7.widget.RecyclerView>
        
    </com.hzw.nestedviewgroup.NestedWebViewRecyclerViewGroup>
```


**code**

```java
//WebView的内容高度不满一屏时，手动设置正确高度
setWebViewContentHeight(int height) ; 

//设置滑动监听
setOnScrollListener(onScrollListener listener)

//获取当前的滑动距离
getCurrentScrollY()

//获取WebView的滑动距离
getWebViewScrollY()

//定位到指定的位置
scrollToPosition(int y)

//WebView和RecyclerView区域的上下切换
scrollToNextView(int rvPosition)


//当你的 RecyclerView 不在当前布局中时，有两种方式可以将 RecyclerView
//和 NestedWebViewRecyclerViewGroup 关联起来

//1. 在 NestedWebViewRecyclerViewGroup 的内部如果在解析布局文件时，如果没有找到 RecyclerView ，
//那么在界面显示时会尝试再次获取 RecyclerView ，这种情况不需要你再做额外的事情

//2. 如果你还有更特殊的用法，情况1 任然没有找到你的 RecyclerView
//那么可以调用setRecyclerView 方法，将两者关联
setRecyclerView(RecyclerView recyclerView)
```



**使用时请注意**：
1. 请为使用到该控件的 Activity 设置以下主题，避免ActionMode向下挤压布局，造成该控件的高度发生变化
```xml
    <item name="windowActionModeOverlay">true</item>
    <item name="android:windowActionModeOverlay">true</item>
```
2. NestedWebViewRecyclerViewGroup只能包含两个子View： NestedScrollWebView 
和 RecyclerView 或包含 RecyclerView 的ViewGroup


</br></br>

License
-------

   Copyright 2019 hzwSunshine

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
