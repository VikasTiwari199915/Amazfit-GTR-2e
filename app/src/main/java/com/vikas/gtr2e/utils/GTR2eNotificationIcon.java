package com.vikas.gtr2e.utils;

/**
 * Provides icon ids for icons on GTR 2e device
 * @author Vikas Tiwari
 */
public class GTR2eNotificationIcon {
    public static final byte WECHAT = 0;
    public static final byte TENCENT_QQ = 1;
    public static final byte MI_CHAT = 2;
    public static final byte FACEBOOK = 3;
    public static final byte TWITTER = 4;
    public static final byte MI_FIT = 5;
    public static final byte SNAPCHAT = 6;
    public static final byte WHATSAPP = 7;
    public static final byte SINA_WEIBO = 8;
    public static final byte TAOBAO = 9;
    public static final byte ALARM_CLOCK = 10;
    public static final byte APP_11 = 11;
    public static final byte INSTAGRAM = 12;
    public static final byte CHAT_BLUE_13 = 13;
    public static final byte QIANNIU = 14;
    public static final byte BAIDU_TIEBA = 15;
    public static final byte ALIPAY = 16;
    public static final byte TENCENT_QZONE = 17;
    public static final byte FLEA_MARKET_IDLEFISH = 18;
    public static final byte JINGDONG_MALL = 19;
    public static final byte DINGTALK = 20;
    public static final byte CALENDAR = 21;
    public static final byte FACEBOOK_MESSENGER = 22;
    public static final byte VIBER = 23;
    public static final byte LINE = 24;
    public static final byte TELEGRAM = 25;
    public static final byte KAKAOTALK = 26;
    public static final byte SKYPE = 27;
    public static final byte VKONTAKTE = 28;
    public static final byte POKEMONGO = 29;
    public static final byte HANGOUTS = 30;
    public static final byte MI_STORE = 31;
    public static final byte GENERIC_APP_32 = 32;
    public static final byte YOUTUBE = 33;
    public static final byte EMAIL = 34;
    public static final byte WEATHER_ALERT = 35;
    public static final byte HR_NUDGED = 36;

    public static byte getIcon(String packageName) {
        return switch (packageName) {
            case "com.fsck.k9", "com.fsck.k9.material", "com.imaeses.squeaky", "com.android.email",
                 "ch.protonmail.android", "security.pEp", "eu.faircode.email",
                 "com.microsoft.office.outlook", "com.yahoo.mobile.client.android.mail",
                 "com.google.android.gm" -> EMAIL;
            case "eu.siacs.conversations", "de.pixart.messenger", "im.vector.alpha", "com.hipchat",
                 "org.kontalk", "chat.tox.antox", "com.moez.QKSMS", "com.android.mms",
                 "com.android.messaging", "com.sonyericsson.conversations",
                 "org.smssecure.smssecure", "com.tencent.mm",
                 "com.google.android.apps.googlevoice" -> WECHAT;
            case "ws.xsoh.etar", "com.android.calendar", "com.google.android.calendar",
                 "mikado.bizcalpro", "com.appgenix.bizcal" -> CALENDAR;
            case "me.zeeroooo.materialfb", "it.rignanese.leo.slimfacebook",
                 "me.jakelane.wrapperforfacebook", "com.facebook.katana",
                 "org.indywidualni.fblite" -> FACEBOOK;
            case "com.facebook.orca", "com.facebook.mlite", "org.thoughtcrime.securesms" ->
                    FACEBOOK_MESSENGER;
            case "com.google.android.talk", "com.google.android.apps.messaging" -> HANGOUTS;
            case "com.instagram.android", "com.google.android.apps.photos" -> INSTAGRAM;
            case "com.kakao.talk" -> KAKAOTALK;
            case "jp.naver.line.android" -> LINE;
            case "com.wire", "ch.threema.app" -> CHAT_BLUE_13;
            case "org.mariotaku.twidere", "com.twitter.android", "org.andstatus.app",
                 "org.mustard.android" -> TWITTER;
            case "com.skype.raider", "com.microsoft.office.lync15" -> SKYPE;
            case "com.snapchat.android" -> SNAPCHAT;
            case "org.telegram.messenger", "org.telegram.messenger.beta",
                 "org.telegram.messenger.web", "org.telegram.plus", "org.thunderdog.challegram",
                 "nekox.messenger" -> TELEGRAM;
            case "com.viber.voip", "com.discord" -> VIBER;
            case "com.whatsapp" -> WHATSAPP;
            case "com.tencent.mobileqq" -> TENCENT_QQ;
            case "com.michatapp.im", "com.michatapp.im.lite" -> MI_CHAT;
            case "com.xiaomi.wearable", "com.xiaomi.hm.health", "com.huami.watch.hmwatchmanager" -> MI_FIT;
            case "com.sina.weibo", "com.weico.international", "com.sina.weibolite" -> SINA_WEIBO;
            case "com.taobao.taobao", "com.taobao.litetao", "com.taobao.live", "com.tmall.wireless",
                 "com.mapplico.tmall", "com.alibaba.intl.android.apps.poseidon",
                 "com.alibaba.aliexpress", "com.alibaba.wireless", "com.tmall.campus.global" -> TAOBAO;
            case "com.taobao.qianniu" -> QIANNIU;
            case "com.baidu.tieba" -> BAIDU_TIEBA;
            case "com.eg.android.AlipayGphone", "com.alipay.alipayhk", "hk.alipay.wallet" -> ALIPAY;
            case "com.qzone", "com.qzone.hd", "com.qzone.lite" -> TENCENT_QZONE;
            case "com.taobao.idlefish", "com.taobao.idlefish.lite", "com.taobao.idlefish.mini" -> FLEA_MARKET_IDLEFISH;
            case "com.jingdong.app.mall" -> JINGDONG_MALL;
            case "com.alibaba.android.rimet" -> DINGTALK;
            case "com.vkontakte.android", "com.arpaplus.kontakt", "com.dctua.android.vkontakte" -> VKONTAKTE;
            case "com.nianticlabs.pokemongo" -> POKEMONGO;
            case "com.xiaomi.market", "com.mi.global.shop" -> MI_STORE;
            case "com.google.android.youtube", "com.google.android.apps.youtube.music",
                 "com.google.android.youtube.googletv","com.google.android.apps.youtube.kids",
                 "com.google.android.apps.youtube.creator" -> YOUTUBE;
            case "gtr2e.app_32" -> GENERIC_APP_32;
            case "gtr2e.weather_alert" -> WEATHER_ALERT; //weather alerts
            case "gtr2e.hrwarning" -> HR_NUDGED; //name nudged you screen
            case "gtr2e.alarmclock" -> ALARM_CLOCK; //shows alarm screen on the device with current time
            default -> APP_11;
        };
    }
}
