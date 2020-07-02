package com.betadata.analytics.android.sdk;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ToggleButton;


import com.betadata.collect.common.AopConstants;

import com.betadata.collect.BetaDataLog;
import com.betadata.collect.BetaDataAPI;
import com.betadata.collect.R;
import com.betadata.collect.ScreenAutoTracker;
import com.betadata.collect.BetaAdapterViewItemTrackProperties;
import com.betadata.collect.BetaDataAutoTrackAppViewScreenUrl;
import com.betadata.collect.BetaDataIgnoreTrackAppViewScreen;
import com.betadata.collect.BetaExpandableListViewItemTrackProperties;
import com.betadata.collect.util.AopUtil;
import com.betadata.collect.util.BetaDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Author: 李巷阳
 * Date: 2019/8/5
 * Version: V2.1.0
 * Part:自动追踪帮助类
 * Description:
 */
@SuppressWarnings("unused")
public class BetaDataAutoTrackHelper {
    private static HashMap<Integer, Long> eventTimestamp = new HashMap<>();

    /**
     * 判断两次点击之间的时间，如果小于500毫秒，则返回true。
     * @param object
     * @return
     */
    private static boolean isDeBounceTrack(Object object) {
        boolean isDeBounceTrack = false;
        long currentOnClickTimestamp = System.currentTimeMillis();
        Object targetObject = eventTimestamp.get(object.hashCode());
        if (targetObject != null) {
            long lastOnClickTimestamp = (long) targetObject;
            if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                isDeBounceTrack = true;
            }
        }

        eventTimestamp.put(object.hashCode(), currentOnClickTimestamp);
        return isDeBounceTrack;
    }

    /**
     * 追踪fragment内容ListView，GridView，Spinner，RadioGroup。
     * @param fragmentName
     * @param root
     */
    private static void traverseView(String fragmentName, ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName)) {
                return;
            }

            if (root == null) {
                return;
            }

            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                if (child instanceof ListView ||
                        child instanceof GridView ||
                        child instanceof Spinner ||
                        child instanceof RadioGroup) {
                    child.setTag(R.id.beta_analytics_tag_view_fragment_name, fragmentName);
                } else if (child instanceof ViewGroup) {
                    traverseView(fragmentName, (ViewGroup) child);
                } else {
                    child.setTag(R.id.beta_analytics_tag_view_fragment_name, fragmentName);
                }
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 判断是否是Fragment
     * @param object
     * @return
     */
    private static boolean isFragment(Object object) {
        try {
            Class<?> supportFragmentClass = null;
            Class<?> androidXFragmentClass = null;
            Class<?> fragment = null;
            try {
                fragment = Class.forName("android.app.Fragment");
            } catch (Exception e) {
                //ignored
            }
            try {
                supportFragmentClass = Class.forName("android.support.v4.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName("androidx.fragment.app.Fragment");
            } catch (Exception e) {
                //ignored
            }

            if (supportFragmentClass == null && androidXFragmentClass == null && fragment == null) {
                return false;
            }

            if ((supportFragmentClass != null && supportFragmentClass.isInstance(object)) ||
                    (androidXFragmentClass != null && androidXFragmentClass.isInstance(object)) ||
                    (fragment != null && fragment.isInstance(object))) {
                return true;
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    /**
     * 追踪fragment创建的埋点
     * @param object
     * @param rootView
     * @param bundle
     */
    public static void onFragmentViewCreated(Object object, View rootView, Bundle bundle) {
        try {
            if (!isFragment(object)) {
                return;
            }

            String fragmentName = object.getClass().getName();

            if (rootView instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) rootView);
            } else {
                rootView.setTag(R.id.beta_analytics_tag_view_fragment_name, fragmentName);
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 自动追踪RN点击的事件
     * @param target
     * @param reactTag
     * @param s
     * @param b
     */
    public static void trackRN(Object target, int reactTag, int s, boolean b) {
        try {
            if (!BetaDataAPI.sharedInstance().isReactNativeAutoTrackEnabled()) {
                return;
            }
            //关闭 AutoTrack
            if (!BetaDataAPI.sharedInstance().isOpenAutoTrack()) {
                return;
            }
            //$AppClick 被过滤
            if (BetaDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }
            JSONObject properties = new JSONObject();
            properties.put(AopConstants.ELEMENT_TYPE, "RNView");
            if (target != null) {
                Class<?> clazz = Class.forName("com.facebook.react.uimanager.NativeViewHierarchyManager");
                Method resolveViewMethod = clazz.getMethod("resolveView", int.class);
                if (resolveViewMethod != null) {
                    Object object = resolveViewMethod.invoke(target, reactTag);
                    if (object != null) {
                        View view = (View) object;
                        //获取所在的 Context
                        Context context = view.getContext();
                        //将 Context 转成 Activity
                        Activity activity = AopUtil.getActivityFromContext(context, view);
                        //$screen_name & $title
                        if (activity != null) {
                            BetaDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
                        }
                        if (view instanceof CompoundButton) {//ReactSwitch
                            return;
                        }
                        if (view instanceof TextView) {
                            TextView textView = (TextView) view;
                            if (!TextUtils.isEmpty(textView.getText())) {
                                properties.put(AopConstants.ELEMENT_CONTENT, textView.getText().toString());
                            }
                        } else if (view instanceof ViewGroup) {
                            StringBuilder stringBuilder = new StringBuilder();
                            String viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                            if (!TextUtils.isEmpty(viewText)) {
                                viewText = viewText.substring(0, viewText.length() - 1);
                            }
                            properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                        }
                    }
                }
            }
            BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_CLICK, properties);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 追踪开启fragment页面
     * @param fragment
     */
    private static void trackFragmentAppViewScreen(Object fragment) {
        try {
            // 是否忽略
            if (!BetaDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
                return;
            }

            if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragment.getClass().getCanonicalName())) {
                return;
            }
            //返回设置 AutoTrack 的 Fragments 集合
            Set<Integer> fragmentsSets = BetaDataAPI.sharedInstance().getAutoTrackFragments();
            // 判断是否追踪此fragment
            boolean isAutoTrackFragment = BetaDataAPI.sharedInstance().isFragmentAutoTrackAppViewScreen(fragment.getClass());
            if (!isAutoTrackFragment) {
                return;
            }
            // 查看注解的忽略的类名
            if (fragment.getClass().getAnnotation(BetaDataIgnoreTrackAppViewScreen.class) != null
                    && fragmentsSets == null) {
                return;
            }

            JSONObject properties = new JSONObject();
            // 获取_title和_screen_name
            AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, null);
            // 判断此fragment是否实现ScreenAutoTracker接口
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;

                String screenUrl = screenAutoTracker.getScreenUrl();
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    BetaDataUtils.mergeJSONObject(otherProperties, properties);
                }

                BetaDataAPI.sharedInstance().trackViewScreen(screenUrl, properties);
            } else {
                // 获取注解时配置的 ScreenUrl。
                BetaDataAutoTrackAppViewScreenUrl autoTrackAppViewScreenUrl = fragment.getClass().getAnnotation(BetaDataAutoTrackAppViewScreenUrl.class);
                if (autoTrackAppViewScreenUrl != null) {
                    String screenUrl = autoTrackAppViewScreenUrl.url();
                    if (TextUtils.isEmpty(screenUrl)) {
                        screenUrl = fragment.getClass().getCanonicalName();
                    }
                    // 发送开启页面事件
                    BetaDataAPI.sharedInstance().trackViewScreen(screenUrl, properties);
                } else {
                    // 发送开启页面事件
                    BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_PAGEVIEW, properties);
                }
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * fragment 可见事件
     * @param object
     */
    public static void trackFragmentResume(Object object) {
        // 是否忽略
        if (!BetaDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            return;
        }

        if (!isFragment(object)) {
            return;
        }

        try {
            Method getParentFragmentMethod = object.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                Object parentFragment = getParentFragmentMethod.invoke(object);
                if (parentFragment == null) {
                    // 如果fragment不可见并且延迟加载
                    if (!fragmentIsHidden(object) && fragmentGetUserVisibleHint(object)) {
                        // 追踪开启fragment页面
                        trackFragmentAppViewScreen(object);
                    }
                } else {
                    if (!fragmentIsHidden(object) && fragmentGetUserVisibleHint(object) && !fragmentIsHidden(parentFragment) && fragmentGetUserVisibleHint(parentFragment)) {
                        // 追踪开启fragment页面
                        trackFragmentAppViewScreen(object);
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * fragment是否延迟加载
     * @param fragment
     * @return
     */
    private static boolean fragmentGetUserVisibleHint(Object fragment) {
        try {
            Method getUserVisibleHintMethod = fragment.getClass().getMethod("getUserVisibleHint");
            if (getUserVisibleHintMethod != null) {
                return (boolean) getUserVisibleHintMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    /**
     * fragment是否可见
     * @param fragment
     * @return
     */
    private static boolean fragmentIsHidden(Object fragment) {
        try {
            Method isHiddenMethod = fragment.getClass().getMethod("isHidden");
            if (isHiddenMethod != null) {
                return (boolean) isHiddenMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    /**
     * 用户开始加载
     * @param object
     * @param isVisibleToUser
     */
    public static void trackFragmentSetUserVisibleHint(Object object, boolean isVisibleToUser) {
        // 是否忽略
        if (!BetaDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            return;
        }

        if (!isFragment(object)) {
            return;
        }

        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = object.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                parentFragment = getParentFragmentMethod.invoke(object);
            }
        } catch (Exception e) {
            //ignored
        }

        if (parentFragment == null) {
            if (isVisibleToUser) {
                if (fragmentIsResumed(object)) {
                    if (!fragmentIsHidden(object)) {
                        trackFragmentAppViewScreen(object);
                    }
                }
            }
        } else {
            if (isVisibleToUser && fragmentGetUserVisibleHint(parentFragment)) {
                if (fragmentIsResumed(object) && fragmentIsResumed(parentFragment)) {
                    if (!fragmentIsHidden(object) && !fragmentIsHidden(parentFragment)) {
                        trackFragmentAppViewScreen(object);
                    }
                }
            }
        }
    }

    /**
     * fragment是否可见
     * @param fragment
     * @return
     */
    private static boolean fragmentIsResumed(Object fragment) {
        try {
            Method isResumedMethod = fragment.getClass().getMethod("isResumed");
            if (isResumedMethod != null) {
                return (boolean) isResumedMethod.invoke(fragment);
            }
        } catch (Exception e) {
            //ignored
        }
        return false;
    }

    /**
     *  监听fragment隐藏
     * @param object
     * @param hidden
     */
    public static void trackOnHiddenChanged(Object object, boolean hidden) {
        if (!BetaDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            return;
        }

        if (!isFragment(object)) {
            return;
        }

        Object parentFragment = null;
        try {
            Method getParentFragmentMethod = object.getClass().getMethod("getParentFragment");
            if (getParentFragmentMethod != null) {
                parentFragment = getParentFragmentMethod.invoke(object);
            }
        } catch (Exception e) {
            //ignored
        }

        if (parentFragment == null) {
            if (!hidden) {
                if (fragmentIsResumed(object)) {
                    if (fragmentGetUserVisibleHint(object)) {
                        trackFragmentAppViewScreen(object);
                    }
                }
            }
        } else {
            if (!hidden && !fragmentIsHidden(parentFragment)) {
                if (fragmentIsResumed(object) && fragmentIsResumed(parentFragment)) {
                    if (fragmentGetUserVisibleHint(object) && fragmentGetUserVisibleHint(parentFragment)) {
                        trackFragmentAppViewScreen(object);
                    }
                }
            }
        }
    }

    /**
     * 自动追踪GroupView的事件
     * @param expandableListView
     * @param view
     * @param groupPosition
     */
    public static void trackExpandableListViewOnGroupClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition) {
        try {
            //关闭 AutoTrack
            if (!BetaDataAPI.sharedInstance().isOpenAutoTrack()) {
                return;
            }

            //$AppClick 被过滤
            if (BetaDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = null;
            if (context instanceof Activity) {
                activity = (Activity) context;
            }

            //Activity 被忽略
            if (activity != null) {
                if (BetaDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            // ExpandableListView Type 被忽略
            if (AopUtil.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            // View 被忽略
            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            JSONObject properties = new JSONObject();

            AopUtil.addViewPathProperties(activity, view, properties);

            if (activity != null) {
                BetaDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }


            String idString = AopUtil.getViewId(expandableListView);
//            if (!TextUtils.isEmpty(idString)) {
//                properties.put(AopConstants.ELEMENT_ID, idString);
//            }
//
//            properties.put(AopConstants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d", groupPosition));
            properties.put(AopConstants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace(e);
                }
            }

            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(expandableListView, properties, activity);

            // 获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.beta_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            // 扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof BetaExpandableListViewItemTrackProperties) {
                    try {
                        BetaExpandableListViewItemTrackProperties trackProperties = (BetaExpandableListViewItemTrackProperties) listAdapter;
                        JSONObject jsonObject = trackProperties.getBetaGroupItemTrackProperties(groupPosition);
                        if (jsonObject != null) {
                            AopUtil.mergeJSONObject(jsonObject, properties);
                        }
                    } catch (JSONException e) {
                        BetaDataLog.printStackTrace(e);
                    }
                }
            }

            BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_CLICK, properties);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 自动追踪ListView嵌套ListView的事件
     * @param expandableListView
     * @param view
     * @param groupPosition
     * @param childPosition
     */
    public static void trackExpandableListViewOnChildClick(ExpandableListView expandableListView, View view,
                                                           int groupPosition, int childPosition) {
        try {
            //关闭 AutoTrack
            if (!BetaDataAPI.sharedInstance().isOpenAutoTrack()) {
                return;
            }

            //$AppClick 被过滤
            if (BetaDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = expandableListView.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, expandableListView);

            //Activity 被忽略
            if (activity != null) {
                if (BetaDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //ExpandableListView 被忽略
            if (AopUtil.isViewIgnored(ExpandableListView.class)) {
                return;
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(expandableListView)) {
                return;
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            //获取 View 自定义属性
            JSONObject properties = (JSONObject) view.getTag(R.id.beta_analytics_tag_view_properties);

            if (properties == null) {
                properties = new JSONObject();
            }

//            properties.put(AopConstants.ELEMENT_POSITION, String.format(Locale.CHINA, "%d:%d", groupPosition, childPosition));

            //扩展属性
            ExpandableListAdapter listAdapter = expandableListView.getExpandableListAdapter();
            if (listAdapter != null) {
                if (listAdapter instanceof BetaExpandableListViewItemTrackProperties) {
                    BetaExpandableListViewItemTrackProperties trackProperties = (BetaExpandableListViewItemTrackProperties) listAdapter;
                    JSONObject jsonObject = trackProperties.getBetaChildItemTrackProperties(groupPosition, childPosition);
                    if (jsonObject != null) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                }
            }

            AopUtil.addViewPathProperties(activity, view, properties);

            if (activity != null) {
                BetaDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

//            //ViewId
//            String idString = AopUtil.getViewId(expandableListView);
//            if (!TextUtils.isEmpty(idString)) {
//                properties.put(AopConstants.ELEMENT_ID, idString);
//            }

//            properties.put(AopConstants.ELEMENT_TYPE, "ExpandableListView");

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace(e);
                }
            }
            //_element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(expandableListView, properties, activity);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.beta_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_CLICK, properties);

        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 自动追踪TabHost的点击事件
     * @param tabName
     */
    public static void trackTabHost(String tabName) {
        try {
            //关闭 AutoTrack
            if (!BetaDataAPI.sharedInstance().isOpenAutoTrack()) {
                return;
            }

            //$AppClick 被过滤
            if (BetaDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //TabHost 被忽略
            if (AopUtil.isViewIgnored(TabHost.class)) {
                return;
            }

            JSONObject properties = new JSONObject();

            properties.put(AopConstants.ELEMENT_CONTENT, tabName);
            properties.put(AopConstants.ELEMENT_TYPE, "TabHost");

            BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_CLICK, properties);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 自动追踪TabLayout点击事件
     * @param object
     * @param tab
     */
    public static void trackTabLayoutSelected(Object object, Object tab) {
        try {
            //关闭 AutoTrack
            if (!BetaDataAPI.sharedInstance().isOpenAutoTrack()) {
                return;
            }

            //App_Click 被过滤
            if (BetaDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            Class<?> supportTabLayoutCLass = null;
            Class<?> androidXTabLayoutCLass = null;
            try {
                supportTabLayoutCLass = Class.forName("android.support.design.widget.TabLayout");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXTabLayoutCLass = Class.forName("com.google.android.material.tabs.TabLayout");
            } catch (Exception e) {
                //ignored
            }

            if (supportTabLayoutCLass == null && androidXTabLayoutCLass == null) {
                return;
            }

            //TabLayout 被忽略
            if (supportTabLayoutCLass != null) {
                if (AopUtil.isViewIgnored(supportTabLayoutCLass)) {
                    return;
                }
            }
            if (androidXTabLayoutCLass != null) {
                if (AopUtil.isViewIgnored(androidXTabLayoutCLass)) {
                    return;
                }
            }

            if (isDeBounceTrack(tab)) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = null;
            boolean isFragment = false;
            if (object instanceof Context) {
                activity = AopUtil.getActivityFromContext((Context) object, null);
            } else {
                try {
                    Field[] fields = object.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        field.setAccessible(true);
                        Object bridgeObject = field.get(object);
                        if (bridgeObject instanceof Activity) {
                            activity = (Activity) bridgeObject;
                            break;
                        } else if (isFragment(bridgeObject)) {
                            object = bridgeObject;
                            isFragment = true;
                            break;
                        } else if (bridgeObject instanceof View) {
                            View view = (View) bridgeObject;
                            activity = AopUtil.getActivityFromContext(view.getContext(), null);
                        }
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace(e);
                }
            }
            //Activity 被忽略
            if (activity != null) {
                if (BetaDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            JSONObject properties = new JSONObject();

            //screen_name & title
            if (isFragment) {
                activity = AopUtil.getActivityFromFragment(object);
                AopUtil.getScreenNameAndTitleFromFragment(properties, object, activity);
            } else if (activity != null) {
                BetaDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            Class<?> supportTabClass = null;
            Class<?> androidXTabClass = null;
            Class<?> currentTabClass;
            try {
                supportTabClass = Class.forName("android.support.design.widget.TabLayout$Tab");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXTabClass = Class.forName("com.google.android.material.tabs.TabLayout$Tab");
            } catch (Exception e) {
                //ignored
            }

            if (supportTabClass != null) {
                currentTabClass = supportTabClass;
            } else {
                currentTabClass = androidXTabClass;
            }

            if (currentTabClass != null) {
                Method method = null;
                try {
                    method = currentTabClass.getMethod("getText");
                } catch (NoSuchMethodException e) {
                    //ignored
                }

                if (method != null) {
                    Object text = method.invoke(tab);

                    //Content
                    if (text != null) {
                        properties.put(AopConstants.ELEMENT_CONTENT, text);
                    }
                }

                if (activity != null) {
                    try {
                        Field field;
                        try {
                            field = currentTabClass.getDeclaredField("mCustomView");
                        } catch (NoSuchFieldException ex) {
                            try {
                                field = currentTabClass.getDeclaredField("customView");
                            } catch (NoSuchFieldException e) {
                                field = null;
                            }
                        }

                        View view = null;
                        if (field != null) {
                            field.setAccessible(true);
                            view = (View) field.get(tab);
                            if (view != null) {
                                try {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    String viewText;
                                    if (view instanceof ViewGroup) {
                                        viewText  = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                                        if (!TextUtils.isEmpty(viewText)) {
                                            viewText = viewText.substring(0, viewText.length() - 1);
                                        }
                                    } else {
                                        viewText = AopUtil.getViewText(view);
                                    }

                                    if (!TextUtils.isEmpty(viewText)) {
                                        properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                                    }
                                } catch (Exception e) {
                                    BetaDataLog.printStackTrace(e);
                                }
                            }
                        }

                        if (view == null || view.getId() == -1) {
                            try {
                                field = currentTabClass.getDeclaredField("mParent");
                            } catch (NoSuchFieldException ex) {
                                field = currentTabClass.getDeclaredField("parent");
                            }
                            field.setAccessible(true);
                            view = (View) field.get(tab);
                        }

//                        String resourceId = activity.getResources().getResourceEntryName(view.getId());
//                        if (!TextUtils.isEmpty(resourceId)) {
//                            properties.put(AopConstants.ELEMENT_ID, resourceId);
//                        }
                    } catch (Exception e) {
                        BetaDataLog.printStackTrace(e);
                    }
                }
            }

//            properties.put(AopConstants.ELEMENT_TYPE, "TabLayout");

            BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_CLICK, properties);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 自动追踪Menu点击事件
     * @param menuItem
     */
    public static void trackMenuItem(MenuItem menuItem) {
        trackMenuItem(null, menuItem);
    }

    /**
     * 自动追踪Menu点击事件
     * @param object
     * @param menuItem
     */
    public static void trackMenuItem(Object object, MenuItem menuItem) {
        try {
            //关闭 AutoTrack
            if (!BetaDataAPI.sharedInstance().isOpenAutoTrack()) {
                return;
            }

            //$AppClick 被过滤
            if (BetaDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //MenuItem 被忽略
            if (AopUtil.isViewIgnored(MenuItem.class)) {
                return;
            }

            if (isDeBounceTrack(menuItem)) {
                return;
            }

            Context context = null;
            if (object != null) {
                if (object instanceof Context) {
                    context = (Context) object;
                }
            }

            //将 Context 转成 Activity
            Activity activity = null;
            if (context != null) {
                activity = AopUtil.getActivityFromContext(context, null);
            }

            //Activity 被忽略
            if (activity != null) {
                if (BetaDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //获取View ID
            String idString = null;
            try {
                if (context != null) {
                    idString = context.getResources().getResourceEntryName(menuItem.getItemId());
                }
            } catch (Exception e) {
                BetaDataLog.printStackTrace(e);
            }

            JSONObject properties = new JSONObject();

            //$screen_name & $title
            if (activity != null) {
                BetaDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

//            //ViewID
//            if (!TextUtils.isEmpty(idString)) {
//                properties.put(AopConstants.ELEMENT_ID, idString);
//            }

            //Content
            if (!TextUtils.isEmpty(menuItem.getTitle())) {
                properties.put(AopConstants.ELEMENT_CONTENT, menuItem.getTitle());
            }

            //Type
//            properties.put(AopConstants.ELEMENT_TYPE, "MenuItem");

            BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_CLICK, properties);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 自动追踪RadioGroup的点击事件
     * @param view
     * @param checkedId
     */
    public static void trackRadioGroup(RadioGroup view, int checkedId) {
        try {
            if (!view.findViewById(checkedId).isPressed()) {
                return;
            }

            //关闭 AutoTrack
            if (!BetaDataAPI.sharedInstance().isOpenAutoTrack()) {
                return;
            }

            //$AppClick 被过滤
            if (BetaDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (BetaDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            JSONObject properties = new JSONObject();

//            //ViewId
//            String idString = AopUtil.getViewId(view);
//            if (!TextUtils.isEmpty(idString)) {
//                properties.put(AopConstants.ELEMENT_ID, idString);
//            }

            //$screen_name & $title
            if (activity != null) {
                BetaDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

//            properties.put(AopConstants.ELEMENT_TYPE, "RadioButton");

            //获取变更后的选中项的ID
            int checkedRadioButtonId = view.getCheckedRadioButtonId();
            if (activity != null) {
                try {
                    RadioButton radioButton = (RadioButton) activity.findViewById(checkedRadioButtonId);
                    if (radioButton != null) {
                        if (!TextUtils.isEmpty(radioButton.getText())) {
                            String viewText = radioButton.getText().toString();
                            if (!TextUtils.isEmpty(viewText)) {
                                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
                            }
                        }
                        AopUtil.addViewPathProperties(activity, radioButton, properties);
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace(e);
                }
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(view, properties, activity);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.beta_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_CLICK, properties);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 自动追踪Dialog的点击事件
     * @param dialogInterface
     * @param whichButton
     */
    public static void trackDialog(DialogInterface dialogInterface, int whichButton) {
        try {
            //关闭 AutoTrack
            if (!BetaDataAPI.sharedInstance().isOpenAutoTrack()) {
                return;
            }

            //$AppClick 被过滤
            if (BetaDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的Context
            Dialog dialog = null;
            if (dialogInterface instanceof Dialog) {
                dialog = (Dialog) dialogInterface;
            }

            if (dialog == null) {
                return;
            }

            if (isDeBounceTrack(dialog)) {
                return;
            }

            Context context = dialog.getContext();

            //将Context转成Activity
            Activity activity = AopUtil.getActivityFromContext(context, null);

            if (activity == null) {
                activity = dialog.getOwnerActivity();
            }

            //Activity 被忽略
            if (activity != null) {
                if (BetaDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //Dialog 被忽略
            if (AopUtil.isViewIgnored(Dialog.class)) {
                return;
            }

            JSONObject properties = new JSONObject();

//            try {
//                if (dialog.getWindow() != null) {
//                    String idString = (String) dialog.getWindow().getDecorView().getTag(R.id.sensors_analytics_tag_view_id);
//                    if (!TextUtils.isEmpty(idString)) {
//                        properties.put(AopConstants.ELEMENT_ID, idString);
//                    }
//                }
//            } catch (Exception e) {
//                BetaDataLog.printStackTrace(e);
//            }

            //$screen_name & $title
            if (activity != null) {
                BetaDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

//            properties.put(AopConstants.ELEMENT_TYPE, "Dialog");

            Class<?> supportAlertDialogClass = null;
            Class<?> androidXAlertDialogClass = null;
            Class<?> currentAlertDialogClass = null;
            try {
                supportAlertDialogClass = Class.forName("android.support.v7.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXAlertDialogClass = Class.forName("androidx.appcompat.app.AlertDialog");
            } catch (Exception e) {
                //ignored
            }

            if (supportAlertDialogClass == null && androidXAlertDialogClass == null) {
                return;
            }

            if (supportAlertDialogClass != null) {
                currentAlertDialogClass = supportAlertDialogClass;
            } else {
                currentAlertDialogClass = androidXAlertDialogClass;
            }

            if (dialog instanceof android.app.AlertDialog) {
                android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
                Button button = alertDialog.getButton(whichButton);
                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                    }
                } else {
                    ListView listView = alertDialog.getListView();
                    if (listView != null) {
                        ListAdapter listAdapter = listView.getAdapter();
                        Object object = listAdapter.getItem(whichButton);
                        if (object != null) {
                            if (object instanceof String) {
                                properties.put(AopConstants.ELEMENT_CONTENT, object);
                            }
                        }
                    }
                }

            } else if (currentAlertDialogClass.isInstance(dialog)) {
                Button button = null;
                try {
                    Method getButtonMethod = dialog.getClass().getMethod("getButton", new Class[]{int.class});
                    if (getButtonMethod != null) {
                        button = (Button) getButtonMethod.invoke(dialog, whichButton);
                    }
                } catch (Exception e) {
                    //ignored
                }

                if (button != null) {
                    if (!TextUtils.isEmpty(button.getText())) {
                        properties.put(AopConstants.ELEMENT_CONTENT, button.getText());
                    }
                } else {
                    try {
                        Method getListViewMethod = dialog.getClass().getMethod("getListView");
                        if (getListViewMethod != null) {
                            ListView listView = (ListView) getListViewMethod.invoke(dialog);
                            if (listView != null) {
                                ListAdapter listAdapter = listView.getAdapter();
                                Object object = listAdapter.getItem(whichButton);
                                if (object != null) {
                                    if (object instanceof String) {
                                        properties.put(AopConstants.ELEMENT_CONTENT, object);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        //ignored
                    }
                }
            }

            BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_CLICK, properties);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 自动追踪ListView的点击事件
     * @param adapterView
     * @param view
     * @param position
     */
    public static void trackListView(AdapterView<?> adapterView, View view, int position) {
        try {
            //闭 AutoTrack
            if (!BetaDataAPI.sharedInstance().isOpenAutoTrack()) {
                return;
            }

            //$AppClick 被过滤
            if (BetaDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();
            if (context == null) {
                return;
            }

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (BetaDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //View Type 被忽略
            if (AopUtil.isViewIgnored(adapterView.getClass())) {
                return;
            }

            JSONObject properties = new JSONObject();

            List<Class> mIgnoredViewTypeList = BetaDataAPI.sharedInstance().getIgnoredViewTypeList();
            if (mIgnoredViewTypeList != null) {
                if (adapterView instanceof ListView) {
//                    properties.put(AopConstants.ELEMENT_TYPE, "ListView");
                    if (AopUtil.isViewIgnored(ListView.class)) {
                        return;
                    }
                } else if (adapterView instanceof GridView) {
//                    properties.put(AopConstants.ELEMENT_TYPE, "GridView");
                    if (AopUtil.isViewIgnored(GridView.class)) {
                        return;
                    }
                }
            }

//            //ViewId
//            String idString = AopUtil.getViewId(adapterView);
//            if (!TextUtils.isEmpty(idString)) {
//                properties.put(AopConstants.ELEMENT_ID, idString);
//            }

            //扩展属性
            Adapter adapter = adapterView.getAdapter();
            if (adapter instanceof HeaderViewListAdapter) {
                adapter = ((HeaderViewListAdapter) adapter).getWrappedAdapter();
            }

            if (adapter instanceof BetaAdapterViewItemTrackProperties) {
                try {
                    BetaAdapterViewItemTrackProperties objectProperties = (BetaAdapterViewItemTrackProperties) adapter;
                    JSONObject jsonObject = objectProperties.getBetaItemTrackProperties(position);
                    if (jsonObject != null) {
                        AopUtil.mergeJSONObject(jsonObject, properties);
                    }
                } catch (JSONException e) {
                    BetaDataLog.printStackTrace(e);
                }
            }

            AopUtil.addViewPathProperties(activity, view, properties);

            //Activity 名称和页面标题
            if (activity != null) {
                BetaDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            //点击的 position
//            properties.put(AopConstants.ELEMENT_POSITION, String.valueOf(position));

            String viewText = null;
            if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace(e);
                }
            } else if (view instanceof TextView) {
                viewText = ((TextView) view).getText().toString();
            }
            //$element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText);
            }

            //fragmentName
            AopUtil.getFragmentNameFromView(adapterView, properties, activity);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.beta_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_CLICK, properties);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 自动追踪Drawer抽屉打开事件
     * @param view
     */
    public static void trackDrawerOpened(View view) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(AopConstants.ELEMENT_CONTENT, "Open");

            BetaDataAPI.sharedInstance().setViewProperties(view, jsonObject);

            trackViewOnClick(view);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }
    /**
     * 自动追踪Drawer抽屉关闭事件
     * @param view
     */
    public static void trackDrawerClosed(View view) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(AopConstants.ELEMENT_CONTENT, "Close");

            BetaDataAPI.sharedInstance().setViewProperties(view, jsonObject);

            trackViewOnClick(view);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 自动追踪按钮点击事件
     * @param view
     */
    public static void trackViewOnClick(View view) {
        try {
            //关闭 AutoTrack
            if (!BetaDataAPI.sharedInstance().isOpenAutoTrack()) {
                return;
            }
            //$AppClick 被过滤
            if (BetaDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType.APP_CLICK)) {
                return;
            }

            //获取所在的 Context
            Context context = view.getContext();

            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, view);

            //Activity 被忽略
            if (activity != null) {
                if (BetaDataAPI.sharedInstance().isActivityAutoTrackAppClickIgnored(activity.getClass())) {
                    return;
                }
            }

            //View 被忽略
            if (AopUtil.isViewIgnored(view)) {
                return;
            }

            long currentOnClickTimestamp = System.currentTimeMillis();
            String tag = (String) view.getTag(R.id.beta_analytics_tag_view_onclick_timestamp);
            if (!TextUtils.isEmpty(tag)) {
                try {
                    long lastOnClickTimestamp = Long.parseLong(tag);
                    if ((currentOnClickTimestamp - lastOnClickTimestamp) < 500) {
                        return;
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace(e);
                }
            }
            view.setTag(R.id.beta_analytics_tag_view_onclick_timestamp, String.valueOf(currentOnClickTimestamp));

            JSONObject properties = new JSONObject();

            AopUtil.addViewPathProperties(activity, view, properties);

//            //ViewId
//            String idString = AopUtil.getViewId(view);
//            if (!TextUtils.isEmpty(idString)) {
//                properties.put(AopConstants.ELEMENT_ID, idString);
//            }

            //$screen_name & $title
            if (activity != null) {
                BetaDataUtils.mergeJSONObject(AopUtil.buildTitleAndScreenName(activity), properties);
            }

            String viewType = view.getClass().getCanonicalName();
            Class<?> androidSwitchClass = null;
            Class<?> supportSwitchCompatClass = null;
            Class<?> androidXSwitchCompatClass = null;
            Class<?> currentSwitchCompatClass = null;

            try {
                androidSwitchClass = Class.forName("android.widget.Switch");
            } catch (Exception e) {
                //ignore
            }

            try {
                supportSwitchCompatClass = Class.forName("android.support.v7.widget.SwitchCompat");
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXSwitchCompatClass = Class.forName("androidx.appcompat.widget.SwitchCompat");
            } catch (Exception e) {
                //ignored
            }

            if (supportSwitchCompatClass != null) {
                currentSwitchCompatClass = supportSwitchCompatClass;
            } else {
                currentSwitchCompatClass = androidXSwitchCompatClass;
            }

            CharSequence viewText = null;
            if (view instanceof CheckBox) { // CheckBox
                if (!view.isPressed()) {
                    return;
                }
                viewType = "CheckBox";
                CheckBox checkBox = (CheckBox) view;
                viewText = checkBox.getText();

            }else if (androidSwitchClass != null && androidSwitchClass.isInstance(view)) {
                if (!view.isPressed()) {
                    return;
                }
                viewType = "Switch";
                viewText = AopUtil.getCompoundButtonText(view);
            } else if (currentSwitchCompatClass != null && currentSwitchCompatClass.isInstance(view)) {
                if (!view.isPressed()) {
                    return;
                }
                viewType = "SwitchCompat";
                viewText = AopUtil.getCompoundButtonText(view);
            } else if (view instanceof RadioButton) { // RadioButton
                viewType = "RadioButton";
                RadioButton radioButton = (RadioButton) view;
                viewText = radioButton.getText();
            } else if (view instanceof ToggleButton) { // ToggleButton
                if (!view.isPressed()) {
                    return;
                }
                viewType = "ToggleButton";
                viewText = AopUtil.getCompoundButtonText(view);
            } else if (view instanceof Button) { // Button
                viewType = "Button";
                Button button = (Button) view;
                viewText = button.getText();
            } else if (view instanceof CheckedTextView) { // CheckedTextView
                viewType = "CheckedTextView";
                CheckedTextView textView = (CheckedTextView) view;
                viewText = textView.getText();
            } else if (view instanceof TextView) { // TextView
                viewType = "TextView";
                TextView textView = (TextView) view;
                viewText = textView.getText();
            } else if (view instanceof ImageView) { // ImageView
                viewType = "ImageView";
                ImageView imageView = (ImageView) view;
                if (!TextUtils.isEmpty(imageView.getContentDescription())) {
                    viewText = imageView.getContentDescription().toString();
                }
            } else if (view instanceof RatingBar) {
                viewType = "RatingBar";
                RatingBar ratingBar = (RatingBar) view;
                viewText = String.valueOf(ratingBar.getRating());
            } else if (view instanceof SeekBar) {
                viewType = "SeekBar";
                SeekBar seekBar = (SeekBar) view;
                viewText = String.valueOf(seekBar.getProgress());
            } else if (view instanceof Spinner) {
                viewType = "Spinner";
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace(e);
                }
            } else if (view instanceof ViewGroup) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    viewText = AopUtil.traverseView(stringBuilder, (ViewGroup) view);
                    if (!TextUtils.isEmpty(viewText)) {
                        viewText = viewText.toString().substring(0, viewText.length() - 1);
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace(e);
                }
            }

            //element_content
            if (!TextUtils.isEmpty(viewText)) {
                properties.put(AopConstants.ELEMENT_CONTENT, viewText.toString());
            }

            //element_type
            properties.put(AopConstants.ELEMENT_TYPE, viewType);

            //fragmentName
            AopUtil.getFragmentNameFromView(view, properties, activity);

            //获取 View 自定义属性
            JSONObject p = (JSONObject) view.getTag(R.id.beta_analytics_tag_view_properties);
            if (p != null) {
                AopUtil.mergeJSONObject(p, properties);
            }

            BetaDataAPI.sharedInstance().track(AopConstants.BETA_APP_CLICK, properties);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    /**
     * 开启track事件
     * @param eventName
     * @param properties
     */
    public static void track(String eventName, String properties) {
        try {
            if (TextUtils.isEmpty(eventName)) {
                return;
            }
            JSONObject pro = null;
            if (!TextUtils.isEmpty(properties)) {
                try {
                    pro = new JSONObject(properties);
                } catch (Exception e) {
                    BetaDataLog.printStackTrace(e);
                }
            }
            BetaDataAPI.sharedInstance().track(eventName, pro);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }
}