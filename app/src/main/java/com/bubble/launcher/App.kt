package com.bubble.launcher

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.InputFilter
import android.text.TextPaint
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import org.json.JSONArray
import org.json.JSONObject

// ===== AppRepo =====
class AppEntry(val label: String, val pkg: String, val icon: Drawable)

class CategoryDef(val name: String, val emoji: String)

/** Loads every launchable app on the phone and sorts it into a category. */
object AppRepo {

    val CATEGORIES = listOf(
        CategoryDef("AI tools", "ai"),
        CategoryDef("Social", "social"),
        CategoryDef("Games", "games"),
        CategoryDef("Shopping", "shopping"),
        CategoryDef("Food", "food"),
        CategoryDef("10-min", "10min"),
        CategoryDef("Payments", "payments"),
        CategoryDef("Banks", "banks"),
        CategoryDef("Calls", "calls"),
        CategoryDef("Media", "media"),
        CategoryDef("Travel", "travel"),
        CategoryDef("Tools", "tools"),
        CategoryDef("Others", "others")
    )

    private val known = HashMap<String, String>()

    private fun add(cat: String, vararg pkgs: String) {
        for (p in pkgs) known[p] = cat
    }

    init {
        add("AI tools", "com.anthropic.claude", "com.openai.chatgpt", "com.google.android.apps.bard",
            "ai.perplexity.app.android", "com.microsoft.copilot", "com.deepseek.chat", "com.x.grok",
            "com.microsoft.bing", "com.quora.poe")
        add("Social", "com.whatsapp", "com.whatsapp.w4b", "com.instagram.android", "com.facebook.katana",
            "com.facebook.orca", "com.twitter.android", "com.snapchat.android", "com.linkedin.android",
            "org.telegram.messenger", "in.mohalla.sharechat", "com.discord", "com.reddit.frontpage",
            "com.instagram.barcelona", "com.pinterest", "org.thoughtcrime.securesms")
        add("Games", "com.pubg.imobile", "com.dts.freefireth", "com.dts.freefiremax", "com.ludo.king",
            "com.king.candycrushsaga", "com.kiloo.subwaysurf", "com.supercell.clashroyale",
            "com.supercell.clashofclans", "com.innersloth.spacemafia", "com.miniclip.eightballpool",
            "com.chess", "com.activision.callofduty.shooter", "com.mojang.minecraftpe")
        add("Shopping", "in.amazon.mShop.android.shopping", "com.flipkart.android", "com.myntra.android",
            "com.meesho.supply", "com.ril.ajio", "com.fsn.nykaa", "com.snapdeal.main", "com.tatadigital.tcp",
            "com.lenskart.app", "com.jio.jiomart")
        add("Food", "in.swiggy.android", "com.application.zomato", "com.done.faasos", "com.Dominos",
            "com.yum.pizzahut", "com.mcdonalds.mobileapp", "com.global.foodpanda.android")
        add("10-min", "com.grofers.customerapp", "com.zeptoconsumerapp", "com.bigbasket.mobileapp",
            "com.jpl.jiomart", "com.flipkart.minutes", "com.dmart.ready")
        add("Payments", "com.phonepe.app", "com.google.android.apps.nbu.paisa.user", "net.one97.paytm",
            "com.dreamplug.androidapp", "in.org.npci.upiapp", "com.mobikwik_new", "money.super.payments",
            "com.naviapp", "com.freecharge.android")
        add("Banks", "com.sbi.lotusintouch", "com.sbi.SBIFreedomPlus", "com.csam.icici.bank.imobile",
            "com.snapwork.hdfc", "com.hdfcbank.payzapp", "com.msf.kbank.mobile", "com.axis.mobile",
            "com.bankofbaroda.mconnect", "com.infrasoft.uboi", "com.canarabank.mobility", "com.idfcfirstbank.optimus")
        add("Calls", "com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer",
            "com.google.android.contacts", "com.android.contacts", "com.samsung.android.app.contacts",
            "com.truecaller", "com.google.android.apps.messaging", "com.android.mms",
            "com.samsung.android.messaging")
        add("Media", "com.google.android.youtube", "com.spotify.music", "com.jio.media.jiobeats",
            "in.startv.hotstar", "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient",
            "com.google.android.apps.youtube.music", "com.gaana", "com.jio.jioplay.tv",
            "com.google.android.apps.photos", "com.mxtech.videoplayer.ad")
        add("Travel", "com.google.android.apps.maps", "com.ubercab", "com.olacabs.customer",
            "in.redbus.android", "com.makemytrip", "cris.org.in.prs.ima", "com.rapido.passenger",
            "com.goibibo", "com.ixigo.train.ixitrain", "com.yatra.base")
        add("Tools", "com.android.settings", "com.google.android.calculator", "com.sec.android.app.popupcalculator",
            "com.google.android.deskclock", "com.google.android.calendar", "com.android.chrome",
            "com.google.android.gm", "com.google.android.apps.docs", "com.google.android.keep",
            "com.android.vending", "com.google.android.googlequicksearchbox", "com.android.camera",
            "com.google.android.GoogleCamera", "com.sec.android.app.camera")
    }

    @Volatile
    var apps: List<AppEntry> = emptyList()
        private set

    @Volatile
    private var catOf: Map<String, String> = emptyMap()

    @Synchronized
    fun load(ctx: Context) {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val list = pm.queryIntentActivities(intent, 0)
        val seen = HashSet<String>()
        val out = ArrayList<AppEntry>()
        val cats = HashMap<String, String>()
        for (ri in list) {
            val ai = ri.activityInfo?.applicationInfo ?: continue
            val pkg = ai.packageName
            if (pkg == ctx.packageName || !seen.add(pkg)) continue
            val label = ri.loadLabel(pm).toString()
            val icon = try {
                ri.loadIcon(pm)
            } catch (e: Exception) {
                continue
            }
            out.add(AppEntry(label, pkg, icon))
            cats[pkg] = classify(pkg, label, ai)
        }
        out.sortBy { it.label.lowercase() }
        catOf = cats
        apps = out
    }

    private fun classify(pkg: String, label: String, ai: ApplicationInfo): String {
        known[pkg]?.let { return it }
        val l = label.lowercase()
        if (l.contains("bank")) return "Banks"
        if (l.contains(" pay") || l.endsWith("pay") || l.contains("upi")) return "Payments"
        if (pkg.contains("dialer") || pkg.contains("contacts") || pkg.contains("messaging") || pkg.endsWith(".mms")) return "Calls"
        if ((ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0) return "Games"
        return when (ai.category) {
            ApplicationInfo.CATEGORY_GAME -> "Games"
            ApplicationInfo.CATEGORY_SOCIAL -> "Social"
            ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> "Media"
            ApplicationInfo.CATEGORY_MAPS -> "Travel"
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Tools"
            else -> {
                val system = listOf("com.android.", "com.google.android.", "com.samsung.", "com.sec.",
                    "com.miui.", "com.xiaomi.", "com.oneplus.", "com.oplus.", "com.coloros.", "com.vivo.",
                    "com.realme.", "com.motorola.", "com.nothing.")
                if (system.any { pkg.startsWith(it) }) "Tools" else "Others"
            }
        }
    }

    fun appsIn(cat: String): List<AppEntry> {
        val map = catOf
        return apps.filter { map[it.pkg] == cat }
    }

    fun byPkg(pkg: String): AppEntry? = apps.firstOrNull { it.pkg == pkg }
}

// ===== Store =====
class CustomCat(var name: String, val pkgs: MutableList<String>)

/** Saves settings and the user's custom categories on the phone. */
object Store {
    fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences("bubble", Context.MODE_PRIVATE)

    fun enabled(ctx: Context) = prefs(ctx).getBoolean("enabled", false)
    fun setEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean("enabled", v).apply()

    fun sound(ctx: Context) = prefs(ctx).getBoolean("sound", true)
    fun setSound(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean("sound", v).apply()

    fun haptics(ctx: Context) = prefs(ctx).getBoolean("haptics", true)
    fun setHaptics(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean("haptics", v).apply()

    fun theme(ctx: Context) = prefs(ctx).getInt("theme", Theme.GLASS)
    fun setTheme(ctx: Context, v: Int) = prefs(ctx).edit().putInt("theme", v).apply()

    fun blur(ctx: Context) = prefs(ctx).getBoolean("blur", true)
    fun setBlur(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean("blur", v).apply()

    fun customs(ctx: Context): MutableList<CustomCat> {
        val s = prefs(ctx).getString("customs", null) ?: return defaults()
        return try {
            val arr = JSONArray(s)
            MutableList(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val p = o.getJSONArray("p")
                CustomCat(o.getString("n"), MutableList(p.length()) { k -> p.getString(k) })
            }
        } catch (e: Exception) {
            defaults()
        }
    }

    fun saveCustoms(ctx: Context, list: List<CustomCat>) {
        val arr = JSONArray()
        for (c in list) {
            val p = JSONArray()
            for (pkg in c.pkgs) p.put(pkg)
            arr.put(JSONObject().put("n", c.name).put("p", p))
        }
        prefs(ctx).edit().putString("customs", arr.toString()).apply()
    }

    private fun defaults(): MutableList<CustomCat> = mutableListOf(
        CustomCat("Work", mutableListOf("com.linkedin.android", "com.google.android.gm", "com.anthropic.claude", "com.openai.chatgpt")),
        CustomCat("Home", mutableListOf("in.swiggy.android", "com.grofers.customerapp", "com.phonepe.app", "com.application.zomato")),
        CustomCat("Daily", mutableListOf("com.whatsapp", "com.instagram.android", "com.google.android.apps.nbu.paisa.user", "com.google.android.youtube"))
    )
}

// ===== Themes =====
object Theme {
    const val GLASS = 0
    const val NINJA = 1
    const val OCEAN = 2
    val NAMES = listOf("Liquid glass", "Ninja", "Ocean")
}

// ===== Icons (hand-drawn line icons on a 24-unit grid) =====
object Icons {
    private val path = Path()
    private val rect = RectF()

    private fun star4(cx: Float, cy: Float, r: Float) {
        path.moveTo(cx, cy - r)
        path.quadTo(cx, cy, cx + r, cy)
        path.quadTo(cx, cy, cx, cy + r)
        path.quadTo(cx, cy, cx - r, cy)
        path.quadTo(cx, cy, cx, cy - r)
        path.close()
    }

    /** Draws icon [key] centred at (cx, cy), [size] wide, with stroke paint [p] (fill is used for dots). */
    fun draw(c: Canvas, key: String, cx: Float, cy: Float, size: Float, p: Paint, fill: Paint) {
        val u = size / 24f
        c.save()
        c.translate(cx - 12 * u, cy - 12 * u)
        c.scale(u, u)
        val sw = p.strokeWidth
        p.strokeWidth = 1.8f
        path.reset()
        when (key) {
            "ai" -> { star4(10f, 13f, 7.5f); star4(18.5f, 5.5f, 3.2f); c.drawPath(path, p) }
            "social" -> {
                rect.set(3f, 4f, 21f, 16f); path.addRoundRect(rect, 4.5f, 4.5f, Path.Direction.CW)
                path.moveTo(8f, 16f); path.lineTo(6.5f, 20.5f); path.lineTo(12f, 16f)
                c.drawPath(path, p)
                c.drawCircle(8f, 10f, 1.2f, fill); c.drawCircle(12f, 10f, 1.2f, fill); c.drawCircle(16f, 10f, 1.2f, fill)
            }
            "games" -> {
                rect.set(2f, 7f, 22f, 18f); path.addRoundRect(rect, 5.5f, 5.5f, Path.Direction.CW)
                path.moveTo(7f, 10f); path.lineTo(7f, 15f); path.moveTo(4.5f, 12.5f); path.lineTo(9.5f, 12.5f)
                c.drawPath(path, p)
                c.drawCircle(16f, 11f, 1.3f, fill); c.drawCircle(18.6f, 13.8f, 1.3f, fill)
            }
            "shopping" -> {
                path.moveTo(5f, 8f); path.lineTo(19f, 8f); path.lineTo(18f, 21f); path.lineTo(6f, 21f); path.close()
                path.moveTo(9f, 8f); path.lineTo(9f, 6.5f); rect.set(9f, 3.5f, 15f, 9.5f); path.arcTo(rect, 180f, 180f); path.lineTo(15f, 8f)
                c.drawPath(path, p)
            }
            "food" -> {
                path.moveTo(7f, 11f); path.lineTo(7f, 21f)
                path.moveTo(5f, 3f); path.lineTo(5f, 8f); path.quadTo(5f, 11f, 7f, 11f); path.quadTo(9f, 11f, 9f, 8f); path.lineTo(9f, 3f)
                path.moveTo(7f, 3f); path.lineTo(7f, 8f)
                path.moveTo(16.5f, 21f); path.lineTo(16.5f, 3f); path.quadTo(20f, 6f, 19.5f, 11f); path.quadTo(19f, 13f, 16.5f, 13f)
                c.drawPath(path, p)
            }
            "10min" -> {
                path.moveTo(13f, 2.5f); path.lineTo(5f, 13.5f); path.lineTo(11.5f, 13.5f); path.lineTo(10.5f, 21.5f)
                path.lineTo(19f, 10.5f); path.lineTo(12.5f, 10.5f); path.close()
                c.drawPath(path, p)
            }
            "payments" -> {
                rect.set(3f, 6f, 21f, 19f); path.addRoundRect(rect, 3f, 3f, Path.Direction.CW)
                path.moveTo(3f, 9.5f); path.lineTo(18f, 9.5f)
                rect.set(14.5f, 11.5f, 21f, 16f); path.addRoundRect(rect, 2f, 2f, Path.Direction.CW)
                c.drawPath(path, p)
                c.drawCircle(17.3f, 13.75f, 1.1f, fill)
            }
            "banks" -> {
                path.moveTo(3f, 9f); path.lineTo(12f, 4f); path.lineTo(21f, 9f); path.close()
                for (x in floatArrayOf(6f, 10f, 14f, 18f)) { path.moveTo(x, 11.5f); path.lineTo(x, 17f) }
                path.moveTo(3.5f, 20f); path.lineTo(20.5f, 20f)
                c.drawPath(path, p)
            }
            "calls" -> {
                path.moveTo(6f, 3f); path.lineTo(9f, 3f); path.lineTo(11f, 8f); path.lineTo(8.5f, 9.5f)
                path.quadTo(10.5f, 13.5f, 14.5f, 15.5f); path.lineTo(16f, 13f); path.lineTo(21f, 15f); path.lineTo(21f, 18.5f)
                path.quadTo(21f, 21f, 18.5f, 21f); path.quadTo(11f, 20.5f, 6.5f, 16f); path.quadTo(3f, 11.5f, 3f, 5.5f)
                path.quadTo(3f, 3f, 6f, 3f); path.close()
                c.drawPath(path, p)
            }
            "media" -> {
                rect.set(3f, 5f, 21f, 19f); path.addRoundRect(rect, 3.5f, 3.5f, Path.Direction.CW)
                path.moveTo(10f, 9f); path.lineTo(15.5f, 12f); path.lineTo(10f, 15f); path.close()
                c.drawPath(path, p)
            }
            "travel" -> {
                path.moveTo(12f, 21.5f); path.lineTo(6.68f, 13.23f)
                rect.set(5.5f, 3f, 18.5f, 16f); path.arcTo(rect, 145f, 250f); path.close()
                path.addCircle(12f, 9.5f, 2.3f, Path.Direction.CW)
                c.drawPath(path, p)
            }
            "tools" -> {
                path.addCircle(12f, 12f, 3f, Path.Direction.CW)
                path.addCircle(12f, 12f, 6.3f, Path.Direction.CW)
                for (k in 0 until 8) {
                    val a = k * PI.toFloat() / 4f
                    path.moveTo(12f + cos(a) * 6.3f, 12f + sin(a) * 6.3f)
                    path.lineTo(12f + cos(a) * 8.8f, 12f + sin(a) * 8.8f)
                }
                c.drawPath(path, p)
            }
            "others" -> {
                for ((x, y) in listOf(4f to 4f, 13.5f to 4f, 4f to 13.5f, 13.5f to 13.5f)) {
                    rect.set(x, y, x + 6.5f, y + 6.5f); path.addRoundRect(rect, 2f, 2f, Path.Direction.CW)
                }
                c.drawPath(path, p)
            }
            "custom" -> {
                for (k in 0 until 10) {
                    val a = -PI.toFloat() / 2f + k * PI.toFloat() / 5f
                    val rr = if (k % 2 == 0) 9.5f else 4.2f
                    val x = 12f + cos(a) * rr
                    val y = 12.8f + sin(a) * rr
                    if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                c.drawPath(path, p)
            }
            "folder" -> {
                path.moveTo(3f, 7f); path.quadTo(3f, 5f, 5f, 5f); path.lineTo(9f, 5f); path.lineTo(11f, 7f); path.lineTo(19f, 7f)
                path.quadTo(21f, 7f, 21f, 9f); path.lineTo(21f, 17f); path.quadTo(21f, 19f, 19f, 19f); path.lineTo(5f, 19f)
                path.quadTo(3f, 19f, 3f, 17f); path.close()
                c.drawPath(path, p)
            }
            "bell" -> {
                path.moveTo(6f, 16f); path.lineTo(6f, 11f); path.quadTo(6f, 5f, 12f, 5f); path.quadTo(18f, 5f, 18f, 11f)
                path.lineTo(18f, 16f); path.lineTo(19.5f, 18f); path.lineTo(4.5f, 18f); path.close()
                path.moveTo(10.3f, 20.8f); path.lineTo(13.7f, 20.8f)
                c.drawPath(path, p)
            }
            "clear" -> {
                path.moveTo(4f, 6.5f); path.lineTo(20f, 6.5f)
                path.moveTo(9f, 6.5f); path.lineTo(9f, 4f); path.lineTo(15f, 4f); path.lineTo(15f, 6.5f)
                path.moveTo(6f, 6.5f); path.lineTo(7f, 20.5f); path.lineTo(17f, 20.5f); path.lineTo(18f, 6.5f)
                path.moveTo(10f, 10.5f); path.lineTo(10f, 16.5f); path.moveTo(14f, 10.5f); path.lineTo(14f, 16.5f)
                c.drawPath(path, p)
            }
            "next" -> { path.moveTo(9f, 5f); path.lineTo(16f, 12f); path.lineTo(9f, 19f); c.drawPath(path, p) }
            "prev" -> { path.moveTo(15f, 5f); path.lineTo(8f, 12f); path.lineTo(15f, 19f); c.drawPath(path, p) }
            else -> { path.moveTo(12f, 5f); path.lineTo(12f, 19f); path.moveTo(5f, 12f); path.lineTo(19f, 12f); c.drawPath(path, p) }
        }
        p.strokeWidth = sw
        c.restore()
    }
}

// ===== BotPainter (the character inside a glass sphere; used by the bubble and the menu centre) =====
class BotPainter(private val d: Float) {
    enum class Mood { IDLE, SLEEP, HAPPY, SURPRISED, DIZZY, MUSIC, WINK }

    companion object {
        val ACT_MS = floatArrayOf(1600f, 1400f, 1000f, 1800f, 1400f, 1800f)
    }

    private val aa = Paint.ANTI_ALIAS_FLAG
    private val fill = Paint(aa)
    private val stroke = Paint(aa).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val textP = Paint(aa).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val rect = RectF()
    private val path = Path()
    private val clip = Path()
    private val rnd = java.util.Random()
    private val metal = Color.rgb(222, 220, 240)
    private val eyeOn = Color.rgb(110, 240, 215)

    private var action = -1
    private var actionStart = 0L
    private var nextAction = SystemClock.uptimeMillis() + 2500
    private var nextBlink = SystemClock.uptimeMillis() + 2000

    /** The whole sphere: interior, character, water (ocean), glass. */
    fun drawSphere(c: Canvas, cx: Float, cy: Float, r: Float, m: Mood, now: Long, theme: Int, badge: Boolean, lookBias: Float) {
        val ts = (now % 600000L) / 1000f
        val sleep = m == Mood.SLEEP
        // soft floating shadow
        fill.color = Color.argb(60, 0, 0, 0)
        fill.setShadowLayer(6 * d, 0f, 2 * d, Color.argb(80, 0, 0, 0))
        c.drawCircle(cx, cy, r, fill)
        fill.clearShadowLayer()
        // interior
        val inner = when (theme) {
            Theme.NINJA -> intArrayOf(Color.argb(215, 70, 52, 48), Color.argb(230, 24, 16, 20))
            Theme.OCEAN -> intArrayOf(Color.argb(200, 150, 205, 240), Color.argb(215, 60, 120, 175))
            else -> intArrayOf(Color.argb(150, 120, 116, 170), Color.argb(190, 30, 28, 52))
        }
        fill.shader = RadialGradient(cx - r * 0.3f, cy - r * 0.4f, r * 1.6f,
            if (sleep) Color.argb(215, 40, 36, 70) else inner[0],
            if (sleep) Color.argb(230, 12, 10, 24) else inner[1], Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, fill)
        fill.shader = null

        c.save()
        clip.reset()
        clip.addCircle(cx, cy, r - d * 0.6f, Path.Direction.CW)
        c.clipPath(clip)
        if (theme == Theme.OCEAN) drawOcean(c, cx, cy, r, m, now, ts, badge)
        else drawRobot(c, cx, cy, r, m, now, ts, theme, badge, lookBias, 1f)
        c.restore()

        // liquid glass on top
        fill.shader = LinearGradient(cx, cy - r, cx, cy + r,
            intArrayOf(Color.argb(80, 255, 255, 255), Color.argb(0, 255, 255, 255), Color.argb(35, 255, 255, 255)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, fill)
        fill.shader = null
        stroke.shader = SweepGradient(cx, cy,
            intArrayOf(Color.argb(90, 255, 255, 255), Color.argb(235, 255, 255, 255), Color.argb(50, 255, 255, 255),
                Color.argb(70, 255, 255, 255), Color.argb(235, 255, 255, 255), Color.argb(50, 255, 255, 255), Color.argb(90, 255, 255, 255)),
            floatArrayOf(0f, 0.125f, 0.3f, 0.5f, 0.625f, 0.8f, 1f))
        stroke.strokeWidth = 1.4f * d
        c.drawCircle(cx, cy, r, stroke)
        stroke.shader = null
        rect.set(cx - r + 3 * d, cy - r + 3 * d, cx + r - 3 * d, cy + r - 3 * d)
        stroke.strokeWidth = 2.2f * d
        stroke.color = Color.argb(150, 255, 255, 255)
        c.drawArc(rect, 205f, 55f, false, stroke)
        fill.color = Color.argb(200, 255, 255, 255)
        c.drawCircle(cx - r * 0.42f, cy - r * 0.6f, 1.5f * d, fill)

        if (badge) {
            val pulse = 1f + 0.12f * sin(ts * 6f)
            val bx = cx + r * 0.72f
            val by = cy - r * 0.72f
            fill.color = Color.rgb(255, 59, 48)
            c.drawCircle(bx, by, 5.5f * d * pulse, fill)
            stroke.strokeWidth = 1.5f * d
            stroke.color = Color.WHITE
            c.drawCircle(bx, by, 5.5f * d * pulse, stroke)
        }
    }

    private fun drawOcean(c: Canvas, cx: Float, cy: Float, r: Float, m: Mood, now: Long, ts: Float, badge: Boolean) {
        val pi = PI.toFloat()
        val level = cy + 0.08f * r
        val waveAt = { x: Float -> level + sin(x / r * 5f + ts * 2.4f) * 0.06f * r + sin(x / r * 9f - ts * 1.7f) * 0.025f * r }
        // little captain bot riding the boat
        val bob = waveAt(cx) - level
        val tilt = (waveAt(cx + 0.3f * r) - waveAt(cx - 0.3f * r)) / (0.6f * r) * 40f
        c.save()
        c.rotate(tilt, cx, level + bob)
        drawRobot(c, cx, cy - 0.32f * r + bob, r * 0.62f, m, now, ts, Theme.OCEAN, badge, 0f, 1f)
        // boat hull
        fill.color = Color.rgb(150, 92, 52)
        path.reset()
        path.moveTo(cx - 0.62f * r, level + bob - 0.08f * r)
        path.lineTo(cx + 0.62f * r, level + bob - 0.08f * r)
        path.lineTo(cx + 0.42f * r, level + bob + 0.16f * r)
        path.lineTo(cx - 0.42f * r, level + bob + 0.16f * r)
        path.close()
        c.drawPath(path, fill)
        fill.color = Color.rgb(196, 130, 78)
        rect.set(cx - 0.62f * r, level + bob - 0.1f * r, cx + 0.62f * r, level + bob - 0.04f * r)
        c.drawRoundRect(rect, 0.03f * r, 0.03f * r, fill)
        // mast, sail and flag
        stroke.strokeWidth = 0.04f * r
        stroke.color = Color.rgb(120, 76, 44)
        c.drawLine(cx + 0.38f * r, level + bob - 0.08f * r, cx + 0.38f * r, level + bob - 0.78f * r, stroke)
        fill.color = Color.argb(240, 255, 252, 240)
        path.reset()
        path.moveTo(cx + 0.4f * r, level + bob - 0.74f * r)
        path.quadTo(cx + 0.72f * r + sin(ts * 3f) * 0.03f * r, level + bob - 0.45f * r, cx + 0.4f * r, level + bob - 0.16f * r)
        path.close()
        c.drawPath(path, fill)
        fill.color = Color.rgb(255, 90, 80)
        path.reset()
        path.moveTo(cx + 0.38f * r, level + bob - 0.8f * r)
        path.lineTo(cx + 0.55f * r + sin(ts * 6f) * 0.03f * r, level + bob - 0.76f * r)
        path.lineTo(cx + 0.38f * r, level + bob - 0.7f * r)
        path.close()
        c.drawPath(path, fill)
        c.restore()
        // water (front layer)
        path.reset()
        path.moveTo(cx - r, cy + r)
        var x = cx - r
        while (x <= cx + r) { path.lineTo(x, waveAt(x)); x += r / 12f }
        path.lineTo(cx + r, cy + r)
        path.close()
        fill.shader = LinearGradient(0f, level - 0.1f * r, 0f, cy + r,
            Color.argb(170, 90, 200, 240), Color.argb(225, 20, 80, 160), Shader.TileMode.CLAMP)
        c.drawPath(path, fill)
        fill.shader = null
        // surface shine and rising bubbles
        stroke.strokeWidth = 0.035f * r
        stroke.color = Color.argb(150, 255, 255, 255)
        path.reset()
        x = cx - r
        path.moveTo(x, waveAt(x))
        while (x <= cx + r) { path.lineTo(x, waveAt(x)); x += r / 12f }
        c.drawPath(path, stroke)
        stroke.strokeWidth = 0.025f * r
        for (k in 0..3) {
            val ph = (ts * 0.35f + k * 0.27f) % 1f
            val bx = cx + (-0.5f + k * 0.33f) * r + sin(ph * 9f + k) * 0.04f * r
            val by = cy + r * 0.9f - ph * (cy + r * 0.9f - level)
            stroke.color = Color.argb((180 * (1f - ph)).toInt(), 255, 255, 255)
            c.drawCircle(bx, by, (0.03f + 0.02f * k % 2) * r, stroke)
        }
        if (m == Mood.SLEEP) {
            for (k in 0..2) {
                val ph = (ts * 0.45f + k / 3f) % 1f
                textP.textSize = (0.16f + ph * 0.16f) * r
                textP.color = Color.argb((230 * (1f - ph)).toInt(), 255, 255, 255)
                c.drawText("z", cx - 0.25f * r - ph * 0.2f * r, cy - 0.55f * r - ph * 0.3f * r, textP)
            }
        }
    }

    private fun drawArm(c: Canvas, sx: Float, sy: Float, side: Float, raise: Float, wig: Float, r: Float) {
        val a = Math.toRadians(35.0 + 115.0 * raise + wig).toFloat()
        val len = 0.3f * r
        val ex = sx + side * sin(a) * len
        val ey = sy + cos(a) * len
        c.drawLine(sx, sy, ex, ey, stroke)
        c.drawCircle(ex, ey, 0.07f * r, fill)
    }

    /** The robot; theme adds a ninja hood/headband/scarf or a captain hat. */
    fun drawRobot(c: Canvas, cx: Float, cy: Float, r: Float, m: Mood, now: Long, ts: Float, theme: Int, badge: Boolean, lookBias: Float, scale: Float) {
        val pi = PI.toFloat()
        var ox = 0f
        var oy = 0f
        var rot = 0f
        var lookX = lookBias * 0.09f * r
        var armL = 0f
        var armR = 0f
        var wig = 0f
        var yawn = 0f
        var knock = -1f

        if (m == Mood.IDLE) {
            if (action < 0 && now > nextAction) { action = rnd.nextInt(6); actionStart = now }
            if (action >= 0) {
                val p = (now - actionStart) / ACT_MS[action]
                if (p >= 1f) {
                    action = -1
                    nextAction = now + 3000 + rnd.nextInt(4000)
                } else when (action) {
                    0 -> lookX = sin(p * 2f * pi) * 0.09f * r
                    1 -> { armR = sin(p * pi); wig = sin(p * 40f) * 18f }
                    2 -> oy = -abs(sin(p * 2f * pi)) * 0.22f * r
                    3 -> {
                        ox = sin(p * pi) * 0.2f * r
                        if (p > 0.3f && p < 0.7f) { armR = 0.65f + 0.3f * abs(sin(p * 60f)); knock = (p - 0.3f) / 0.4f }
                    }
                    4 -> rot = sin(p * 2f * pi) * 14f
                    5 -> yawn = sin(p * pi)
                }
            }
            oy += sin(ts * 2f) * 0.03f * r
        } else {
            action = -1
        }
        when (m) {
            Mood.SLEEP -> { oy += sin(ts * 1.2f) * 0.05f * r + 0.06f * r; rot = 8f }
            Mood.MUSIC -> { oy -= abs(sin(ts * pi * 2.2f)) * 0.1f * r; rot = sin(ts * pi * 1.1f) * 9f; armL = 0.5f + 0.3f * sin(ts * 7f); armR = 0.5f - 0.3f * sin(ts * 7f) }
            Mood.DIZZY -> { rot = sin(ts * 12f) * 12f; ox = sin(ts * 9f) * 0.05f * r }
            Mood.HAPPY -> { oy -= abs(sin(ts * 5f)) * 0.05f * r; armL = 0.8f; armR = 0.8f; wig = sin(ts * 14f) * 8f }
            Mood.SURPRISED -> { oy -= 0.08f * r; armL = 0.6f; armR = 0.6f }
            Mood.WINK -> { armR = 0.9f; wig = sin(ts * 20f) * 15f }
            Mood.IDLE -> { }
        }

        var blink = false
        if (now > nextBlink) {
            if (now > nextBlink + 130) nextBlink = now + 2500 + rnd.nextInt(2500) else blink = true
        }

        val hx = cx + ox
        val hy = cy + oy + 0.08f * r
        val hw = 0.88f * r
        val hh = 0.64f * r
        c.save()
        c.rotate(rot, hx, hy)

        val body = if (theme == Theme.NINJA) Color.rgb(46, 44, 70) else metal
        // ninja scarf tail (behind)
        if (theme == Theme.NINJA) {
            fill.color = Color.rgb(196, 48, 48)
            path.reset()
            path.moveTo(hx + hw * 0.45f, hy - 0.2f * r)
            path.quadTo(hx + hw * 0.9f, hy - 0.3f * r + sin(ts * 6f) * 0.06f * r, hx + hw * 1.15f, hy - 0.1f * r + sin(ts * 6f + 1f) * 0.08f * r)
            path.lineTo(hx + hw * 1.1f, hy + 0.02f * r + sin(ts * 6f + 1.5f) * 0.08f * r)
            path.quadTo(hx + hw * 0.85f, hy - 0.1f * r, hx + hw * 0.45f, hy - 0.08f * r)
            path.close()
            c.drawPath(path, fill)
        }
        fill.color = body
        rect.set(hx - 0.3f * r, hy + hh * 0.42f, hx + 0.3f * r, hy + hh * 0.42f + 0.42f * r)
        c.drawRoundRect(rect, 0.12f * r, 0.12f * r, fill)
        stroke.strokeWidth = 0.09f * r
        stroke.color = body
        drawArm(c, hx - 0.3f * r, hy + hh * 0.62f, -1f, armL, -wig * 0.3f, r)
        drawArm(c, hx + 0.3f * r, hy + hh * 0.62f, 1f, armR, wig, r)

        val top = hy - hh / 2f
        val wob = sin(ts * 3f) * 0.06f * r
        if (theme != Theme.OCEAN) {
            stroke.strokeWidth = 0.05f * r
            stroke.color = if (theme == Theme.NINJA) Color.rgb(90, 88, 110) else metal
            c.drawLine(hx, top, hx + wob, top - 0.24f * r, stroke)
            fill.color = when {
                badge -> if (sin(ts * 8f) > 0f) Color.rgb(255, 59, 48) else Color.rgb(140, 30, 30)
                m == Mood.SLEEP -> Color.rgb(90, 88, 110)
                m == Mood.MUSIC -> Color.HSVToColor(floatArrayOf((ts * 90f) % 360f, 0.6f, 1f))
                else -> eyeOn
            }
            c.drawCircle(hx + wob, top - 0.26f * r, 0.07f * r, fill)
        }

        // head
        if (theme == Theme.NINJA) {
            fill.shader = LinearGradient(hx, hy - hh / 2f, hx, hy + hh / 2f, Color.rgb(70, 66, 100), Color.rgb(34, 32, 52), Shader.TileMode.CLAMP)
        } else {
            fill.shader = LinearGradient(hx, hy - hh / 2f, hx, hy + hh / 2f, Color.rgb(250, 250, 255), Color.rgb(200, 197, 230), Shader.TileMode.CLAMP)
        }
        rect.set(hx - hw / 2f, hy - hh / 2f, hx + hw / 2f, hy + hh / 2f)
        c.drawRoundRect(rect, 0.22f * r, 0.22f * r, fill)
        fill.shader = null
        fill.color = if (theme == Theme.NINJA) Color.rgb(30, 28, 46) else Color.rgb(176, 172, 222)
        rect.set(hx - hw / 2f - 0.07f * r, hy - 0.1f * r, hx - hw / 2f + 0.02f * r, hy + 0.1f * r)
        c.drawRoundRect(rect, 0.04f * r, 0.04f * r, fill)
        rect.set(hx + hw / 2f - 0.02f * r, hy - 0.1f * r, hx + hw / 2f + 0.07f * r, hy + 0.1f * r)
        c.drawRoundRect(rect, 0.04f * r, 0.04f * r, fill)

        if (m == Mood.MUSIC) {
            stroke.color = Color.rgb(83, 74, 183)
            stroke.strokeWidth = 0.08f * r
            rect.set(hx - hw / 2f - 0.02f * r, hy - hh / 2f - 0.18f * r, hx + hw / 2f + 0.02f * r, hy + 0.3f * r)
            c.drawArc(rect, 200f, 140f, false, stroke)
            fill.color = Color.rgb(83, 74, 183)
            rect.set(hx - hw / 2f - 0.12f * r, hy - 0.14f * r, hx - hw / 2f + 0.05f * r, hy + 0.14f * r)
            c.drawRoundRect(rect, 0.06f * r, 0.06f * r, fill)
            rect.set(hx + hw / 2f - 0.05f * r, hy - 0.14f * r, hx + hw / 2f + 0.12f * r, hy + 0.14f * r)
            c.drawRoundRect(rect, 0.06f * r, 0.06f * r, fill)
        }

        // face screen
        fill.color = Color.rgb(24, 22, 48)
        rect.set(hx - hw / 2f + 0.09f * r, hy - hh / 2f + 0.09f * r, hx + hw / 2f - 0.09f * r, hy + hh / 2f - 0.09f * r)
        c.drawRoundRect(rect, 0.15f * r, 0.15f * r, fill)

        // eyes
        val eyeCol = when {
            m == Mood.SLEEP -> Color.rgb(70, 140, 130)
            theme == Theme.NINJA -> Color.rgb(255, 196, 90)
            else -> eyeOn
        }
        fill.color = eyeCol
        stroke.color = eyeCol
        stroke.strokeWidth = 0.055f * r
        val ey = hy - 0.03f * r
        for (side in intArrayOf(-1, 1)) {
            val ex = hx + side * 0.19f * r + lookX
            val kind = when (m) {
                Mood.SLEEP -> 1
                Mood.HAPPY, Mood.MUSIC -> 2
                Mood.SURPRISED -> 3
                Mood.DIZZY -> 4
                Mood.WINK -> if (side < 0) 2 else 0
                Mood.IDLE -> 0
            }
            when (kind) {
                0 -> {
                    var eh = if (blink) 0.03f * r else 0.2f * r
                    eh *= (1f - 0.7f * yawn)
                    rect.set(ex - 0.065f * r, ey - eh / 2f, ex + 0.065f * r, ey + eh / 2f)
                    c.drawRoundRect(rect, 0.05f * r, 0.05f * r, fill)
                }
                1 -> { rect.set(ex - 0.08f * r, ey - 0.08f * r, ex + 0.08f * r, ey + 0.06f * r); c.drawArc(rect, 20f, 140f, false, stroke) }
                2 -> { rect.set(ex - 0.08f * r, ey - 0.04f * r, ex + 0.08f * r, ey + 0.12f * r); c.drawArc(rect, 200f, 140f, false, stroke) }
                3 -> c.drawCircle(ex, ey, 0.1f * r, fill)
                4 -> {
                    val k = 0.065f * r
                    c.drawLine(ex - k, ey - k, ex + k, ey + k, stroke)
                    c.drawLine(ex - k, ey + k, ex + k, ey - k, stroke)
                }
            }
        }

        // mouth (the ninja wears a face mask instead)
        val my = hy + 0.15f * r
        if (theme == Theme.NINJA) {
            fill.color = Color.rgb(36, 34, 54)
            rect.set(hx - hw / 2f + 0.08f * r, hy + 0.06f * r, hx + hw / 2f - 0.08f * r, hy + hh / 2f - 0.06f * r)
            c.drawRoundRect(rect, 0.12f * r, 0.12f * r, fill)
            // headband with a plain metal plate and a shuriken mark
            fill.color = Color.rgb(196, 48, 48)
            rect.set(hx - hw / 2f - 0.02f * r, top + 0.02f * r, hx + hw / 2f + 0.02f * r, top + 0.13f * r)
            c.drawRect(rect, fill)
            fill.shader = LinearGradient(hx, top, hx, top + 0.15f * r, Color.rgb(230, 232, 240), Color.rgb(150, 154, 170), Shader.TileMode.CLAMP)
            rect.set(hx - 0.2f * r, top - 0.005f * r, hx + 0.2f * r, top + 0.15f * r)
            c.drawRoundRect(rect, 0.03f * r, 0.03f * r, fill)
            fill.shader = null
            fill.color = Color.rgb(60, 62, 80)
            path.reset()
            val sx = hx
            val sy = top + 0.072f * r
            for (k in 0 until 8) {
                val a = k * pi / 4f + ts * 0.5f
                val rr = if (k % 2 == 0) 0.055f * r else 0.018f * r
                val px = sx + cos(a) * rr
                val py = sy + sin(a) * rr
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            c.drawPath(path, fill)
        } else {
            when (m) {
                Mood.IDLE -> if (yawn > 0.05f) {
                    rect.set(hx - 0.05f * r, my - 0.06f * r * yawn, hx + 0.05f * r, my + 0.06f * r * yawn)
                    c.drawOval(rect, fill)
                } else {
                    rect.set(hx - 0.09f * r, my - 0.07f * r, hx + 0.09f * r, my + 0.03f * r)
                    c.drawArc(rect, 20f, 140f, false, stroke)
                }
                Mood.SLEEP -> c.drawCircle(hx, my, 0.035f * r, fill)
                Mood.HAPPY, Mood.MUSIC, Mood.WINK -> {
                    rect.set(hx - 0.1f * r, my - 0.07f * r, hx + 0.1f * r, my + 0.07f * r)
                    c.drawArc(rect, 0f, 180f, true, fill)
                }
                Mood.SURPRISED -> c.drawCircle(hx, my + 0.01f * r, 0.055f * r, stroke)
                Mood.DIZZY -> {
                    path.reset()
                    path.moveTo(hx - 0.1f * r, my)
                    path.quadTo(hx - 0.05f * r, my - 0.05f * r, hx, my)
                    path.quadTo(hx + 0.05f * r, my + 0.05f * r, hx + 0.1f * r, my)
                    c.drawPath(path, stroke)
                }
            }
            if (m == Mood.HAPPY || m == Mood.MUSIC || m == Mood.WINK) {
                fill.color = Color.argb(150, 255, 120, 160)
                c.drawCircle(hx - 0.3f * r, hy + 0.08f * r, 0.05f * r, fill)
                c.drawCircle(hx + 0.3f * r, hy + 0.08f * r, 0.05f * r, fill)
            }
        }

        // captain hat (ocean)
        if (theme == Theme.OCEAN) {
            fill.color = Color.rgb(250, 250, 252)
            rect.set(hx - 0.36f * r, top - 0.2f * r, hx + 0.36f * r, top + 0.06f * r)
            c.drawRoundRect(rect, 0.12f * r, 0.12f * r, fill)
            fill.color = Color.rgb(28, 40, 72)
            rect.set(hx - 0.42f * r, top + 0.0f * r, hx + 0.42f * r, top + 0.08f * r)
            c.drawRoundRect(rect, 0.04f * r, 0.04f * r, fill)
            fill.color = Color.rgb(250, 196, 60)
            c.drawCircle(hx, top - 0.08f * r, 0.05f * r, fill)
        }
        c.restore()

        when (m) {
            Mood.SLEEP -> if (theme != Theme.OCEAN) for (k in 0..2) {
                val ph = (ts * 0.45f + k / 3f) % 1f
                textP.textSize = (0.2f + ph * 0.22f) * r
                textP.color = Color.argb((230 * (1f - ph)).toInt(), 255, 255, 255)
                c.drawText("z", hx + 0.25f * r + ph * 0.3f * r, top - ph * 0.55f * r, textP)
            }
            Mood.MUSIC -> for (k in 0..1) {
                val ph = (ts * 0.6f + k / 2f) % 1f
                textP.textSize = 0.32f * r
                textP.color = Color.argb((240 * (1f - ph)).toInt(), 140, 230, 255)
                val nx = hx + (if (k == 0) -0.45f else 0.45f) * r + sin(ph * 6f) * 0.06f * r
                c.drawText("♪", nx, top + 0.1f * r - ph * 0.6f * r, textP)
            }
            Mood.DIZZY -> for (k in 0..2) {
                val a = ts * 6f + k * 2.094f
                fill.color = Color.rgb(255, 214, 90)
                c.drawCircle(hx + cos(a) * 0.45f * r, top - 0.05f * r + sin(a) * 0.12f * r, 0.05f * r, fill)
            }
            else -> { }
        }
        if (knock >= 0f) {
            stroke.strokeWidth = 0.04f * r
            stroke.color = Color.argb((200 * (1f - knock)).toInt(), 255, 255, 255)
            c.drawCircle(cx + 0.85f * r, hy, 0.1f * r + knock * 0.25f * r, stroke)
        }
    }
}

// ===== BubbleView (the floating sphere) =====
class BubbleView(ctx: Context) : View(ctx) {
    companion object {
        const val SLEEP_MS = 10000L
    }

    private val d = resources.displayMetrics.density
    private val bot = BotPainter(d)

    var theme = Theme.GLASS
        set(v) { if (field != v) { field = v; invalidate() } }
    var menuOpen = false
        set(v) { field = v; lastActive = SystemClock.uptimeMillis(); invalidate() }
    var dragging = false
        set(v) { field = v; lastActive = SystemClock.uptimeMillis(); invalidate() }
    var musicOn = false
        set(v) { if (field != v) { field = v; invalidate() } }
    var badge = false
        set(v) { if (field != v) { field = v; invalidate() } }
    var pressedLook = false
        set(v) { if (field != v) { field = v; invalidate() } }

    private var lastActive = SystemClock.uptimeMillis()
    private var temp: BotPainter.Mood? = null
    private var tempUntil = 0L

    fun touch() {
        val now = SystemClock.uptimeMillis()
        val asleep = mood(now) == BotPainter.Mood.SLEEP
        lastActive = now
        if (asleep) flash(BotPainter.Mood.SURPRISED, 700) else invalidate()
    }

    fun flash(m: BotPainter.Mood, ms: Long) {
        val now = SystemClock.uptimeMillis()
        temp = m
        tempUntil = now + ms
        lastActive = now
        invalidate()
    }

    private fun mood(now: Long): BotPainter.Mood {
        val t = temp
        if (t != null && now < tempUntil) return t
        if (dragging) return BotPainter.Mood.DIZZY
        if (menuOpen) return BotPainter.Mood.HAPPY
        if (musicOn) return BotPainter.Mood.MUSIC
        if (now - lastActive > SLEEP_MS) return BotPainter.Mood.SLEEP
        return BotPainter.Mood.IDLE
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val now = SystemClock.uptimeMillis()
        val m = mood(now)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) - 6 * d
        val s = if (pressedLook) 0.9f else 1f
        c.save()
        c.scale(s, s, cx, cy)
        bot.drawSphere(c, cx, cy, r, m, now, theme, badge, 0f)
        c.restore()
        if (m == BotPainter.Mood.SLEEP) postInvalidateDelayed(90) else postInvalidateDelayed(33)
    }
}

// ===== SoundFx =====
/** Short, crisp synthesized UI sounds (no audio files). Silent when the phone is on silent/vibrate. */
class SoundFx(ctx: Context, val theme: Int) {
    private val rate = 44100
    private val tracks = HashMap<String, AudioTrack>()
    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    var enabled = true

    /** type 0 = sine sweep, 1 = high-passed noise click, 2 = swept noise whoosh. tau = decay time in seconds. */
    private class Comp(val type: Int, val f1: Double, val f2: Double, val dur: Double, val gain: Double, val tau: Double, val delay: Double = 0.0)

    /** Metallic "ting": inharmonic partials like a blade or kunai. */
    private fun ting(g: Double, tau: Double, delay: Double, pitch: Double) = arrayOf(
        Comp(0, 3150.0 * pitch, 3140.0 * pitch, tau * 5, g, tau, delay),
        Comp(0, 4720.0 * pitch, 4705.0 * pitch, tau * 4, g * 0.7, tau * 0.8, delay),
        Comp(0, 6890.0 * pitch, 6870.0 * pitch, tau * 3, g * 0.45, tau * 0.6, delay),
        Comp(0, 8210.0 * pitch, 8190.0 * pitch, tau * 3, g * 0.3, tau * 0.5, delay),
        Comp(1, 0.0, 0.0, 0.006, g * 0.9, 0.0015, delay)
    )

    private fun plip(g: Double, f: Double, delay: Double) = Comp(0, f, f * 1.9, 0.05, g, 0.016, delay)

    init {
        try {
            when (theme) {
                Theme.NINJA -> {
                    put("tick", render(*ting(0.09, 0.03, 0.0, 1.0)))
                    put("open", render(Comp(2, 300.0, 2600.0, 0.26, 0.28, 1.0), *ting(0.13, 0.08, 0.17, 1.0)))
                    put("sub", render(Comp(2, 600.0, 3000.0, 0.15, 0.2, 1.0), *ting(0.1, 0.05, 0.09, 1.12)))
                    put("close", render(Comp(2, 2600.0, 400.0, 0.22, 0.24, 1.0)))
                    put("page", render(*ting(0.11, 0.05, 0.0, 1.0), *ting(0.11, 0.06, 0.07, 1.15)))
                    put("launch", render(*ting(0.15, 0.16, 0.0, 1.0), *ting(0.12, 0.18, 0.025, 0.83), Comp(1, 0.0, 0.0, 0.012, 0.25, 0.003)))
                    put("drop", render(*ting(0.08, 0.04, 0.0, 0.7)))
                }
                Theme.OCEAN -> {
                    put("tick", render(Comp(0, 1100.0, 2100.0, 0.04, 0.16, 0.012)))
                    put("open", render(plip(0.16, 700.0, 0.0), plip(0.13, 980.0, 0.06), plip(0.11, 1300.0, 0.13), Comp(2, 200.0, 900.0, 0.25, 0.12, 1.0)))
                    put("sub", render(plip(0.15, 850.0, 0.0), plip(0.1, 1250.0, 0.05)))
                    put("close", render(Comp(0, 900.0, 300.0, 0.12, 0.17, 0.05)))
                    put("page", render(plip(0.14, 1000.0, 0.0), plip(0.14, 1400.0, 0.07)))
                    put("launch", render(Comp(2, 180.0, 1200.0, 0.3, 0.2, 1.0), plip(0.12, 900.0, 0.08), plip(0.1, 1350.0, 0.16)))
                    put("drop", render(Comp(0, 500.0, 250.0, 0.1, 0.2, 0.04)))
                }
                else -> {
                    val tick = arrayOf(
                        Comp(0, 4200.0, 3900.0, 0.030, 0.22, 0.004),
                        Comp(0, 1900.0, 1850.0, 0.020, 0.10, 0.003),
                        Comp(1, 0.0, 0.0, 0.008, 0.14, 0.0015)
                    )
                    put("tick", render(*tick))
                    put("open", render(Comp(0, 520.0, 780.0, 0.10, 0.17, 0.03), Comp(0, 1560.0, 1700.0, 0.07, 0.05, 0.015), *tick))
                    put("sub", render(Comp(0, 660.0, 880.0, 0.08, 0.13, 0.022), *tick))
                    put("close", render(Comp(0, 700.0, 420.0, 0.10, 0.14, 0.03), Comp(1, 0.0, 0.0, 0.006, 0.08, 0.0015)))
                    put("page", render(*tick, Comp(0, 4600.0, 4300.0, 0.03, 0.18, 0.004, 0.07), Comp(1, 0.0, 0.0, 0.008, 0.10, 0.0015, 0.07)))
                    put("launch", render(Comp(0, 880.0, 880.0, 0.30, 0.12, 0.07), Comp(0, 1320.0, 1320.0, 0.35, 0.09, 0.08, 0.05), Comp(0, 1760.0, 1760.0, 0.30, 0.04, 0.06, 0.05)))
                    put("drop", render(Comp(0, 240.0, 160.0, 0.09, 0.20, 0.025), Comp(1, 0.0, 0.0, 0.006, 0.08, 0.0015)))
                }
            }
        } catch (e: Exception) {
            // Sound is optional; ignore failures.
        }
    }

    private fun render(vararg cs: Comp): ShortArray {
        var total = 0.0
        for (c in cs) total = maxOf(total, c.delay + c.dur)
        val n = ((total + 0.01) * rate).toInt()
        val out = DoubleArray(n)
        val rnd = java.util.Random(11)
        for (c in cs) {
            val start = (c.delay * rate).toInt()
            val len = (c.dur * rate).toInt()
            var phase = 0.0
            var lp = 0.0
            var lp2 = 0.0
            for (i in 0 until len) {
                val t = i.toDouble() / rate
                val env = if (c.type == 2) sin(PI * t / c.dur).pow(1.5) else min(1.0, t / 0.0015) * exp(-t / c.tau)
                val v = when (c.type) {
                    1 -> {
                        val x = rnd.nextDouble() * 2 - 1
                        lp += 0.35 * (x - lp)
                        x - lp
                    }
                    2 -> {
                        val f = c.f1 * (c.f2 / c.f1).pow(t / c.dur)
                        val a = min(1.0, 2 * PI * f / rate)
                        val x = rnd.nextDouble() * 2 - 1
                        lp += a * (x - lp)
                        lp2 += a * 0.5 * (lp - lp2)
                        (lp - lp2) * 3.0
                    }
                    else -> {
                        val f = c.f1 * (c.f2 / c.f1).pow(t / c.dur)
                        phase += 2 * PI * f / rate
                        sin(phase)
                    }
                }
                val idx = start + i
                if (idx < n) out[idx] += v * env * c.gain
            }
        }
        return ShortArray(n) { (tanh(out[it] * 1.2) * 32767 * 0.9).toInt().toShort() }
    }

    private fun put(name: String, data: ShortArray) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val fmt = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(rate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(fmt)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(data.size * 2)
            .build()
        track.write(data, 0, data.size)
        tracks[name] = track
    }

    fun play(name: String) {
        if (!enabled || am.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val t = tracks[name] ?: return
        try {
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.reloadStaticData()
            t.play()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun release() {
        for (t in tracks.values) {
            try { t.release() } catch (e: Exception) { }
        }
        tracks.clear()
    }
}

/** Light vibration feedback. */
class Haptics(ctx: Context) {
    @Suppress("DEPRECATION")
    private val vib = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    var enabled = true

    private fun fire(predefined: Int, ms: Long, amp: Int) {
        if (!enabled) return
        val v = vib ?: return
        try {
            if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(predefined))
            else v.vibrate(VibrationEffect.createOneShot(ms, amp))
        } catch (e: Exception) {
            // ignore
        }
    }

    fun tick() = fire(VibrationEffect.EFFECT_TICK, 8, 60)
    fun click() = fire(VibrationEffect.EFFECT_CLICK, 14, 120)
    fun heavy() = fire(VibrationEffect.EFFECT_HEAVY_CLICK, 22, 200)
}

// ===== Notifications and music =====
class NotifChat(val title: String, val lines: List<String>)

class AppNotifs(val pkg: String, val label: String) {
    val keys = ArrayList<String>()
    val chats = ArrayList<NotifChat>()
    var intent: PendingIntent? = null
    var messaging = false
    val count: Int get() = chats.size
}

/** Reads notifications (for the red dot and message previews) and gives access to music sessions. */
class BubbleNotifListener : NotificationListenerService() {
    companion object {
        @Volatile
        var instance: BubbleNotifListener? = null

        @Volatile
        var groups: List<AppNotifs> = emptyList()

        var onChange: (() -> Unit)? = null

        val MESSAGING = setOf(
            "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "org.thoughtcrime.securesms",
            "com.google.android.apps.messaging", "com.samsung.android.messaging", "com.android.mms",
            "com.facebook.orca", "com.instagram.android", "com.snapchat.android", "com.discord",
            "com.linkedin.android", "com.microsoft.teams", "com.Slack", "com.truecaller"
        )

        fun isEnabled(ctx: Context): Boolean {
            val s = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
            return s.contains(ctx.packageName)
        }

        fun clearPkg(pkg: String) {
            val i = instance ?: return
            val g = groups.firstOrNull { it.pkg == pkg } ?: return
            try { i.cancelNotifications(g.keys.toTypedArray()) } catch (e: Exception) { }
            groups = groups.filter { it.pkg != pkg }
            i.fire()
        }

        fun clearAll() {
            val i = instance ?: return
            try { i.cancelAllNotifications() } catch (e: Exception) { }
            groups = emptyList()
            i.fire()
        }
    }

    override fun onListenerConnected() {
        instance = this
        rebuild()
    }

    override fun onListenerDisconnected() {
        instance = null
        groups = emptyList()
        fire()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = rebuild()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = rebuild()

    fun fire() {
        val cb = onChange ?: return
        Handler(Looper.getMainLooper()).post { cb() }
    }

    private fun labelOf(pkg: String): String = try {
        @Suppress("DEPRECATION")
        val ai = packageManager.getApplicationInfo(pkg, 0)
        packageManager.getApplicationLabel(ai).toString()
    } catch (e: Exception) {
        AppRepo.byPkg(pkg)?.label ?: pkg
    }

    private fun rebuild() {
        val arr = try { activeNotifications } catch (e: Exception) { null } ?: return
        val map = LinkedHashMap<String, AppNotifs>()
        val summaries = ArrayList<StatusBarNotification>()
        for (sbn in arr.sortedByDescending { it.postTime }) {
            val n = sbn.notification ?: continue
            if (sbn.packageName == packageName || !sbn.isClearable) continue
            if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) { summaries.add(sbn); continue }
            if (n.category == Notification.CATEGORY_TRANSPORT || n.category == Notification.CATEGORY_PROGRESS) continue
            val ex = n.extras ?: continue
            val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val g = map.getOrPut(sbn.packageName) {
                AppNotifs(sbn.packageName, labelOf(sbn.packageName)).also { it.intent = n.contentIntent }
            }
            g.keys.add(sbn.key)
            if (sbn.packageName in MESSAGING || n.category == Notification.CATEGORY_MESSAGE) g.messaging = true
            val lines = ArrayList<String>()
            @Suppress("DEPRECATION")
            val msgs = ex.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (msgs != null && msgs.isNotEmpty()) {
                for (p in msgs) {
                    val b = p as? Bundle ?: continue
                    val t = b.getCharSequence("text")?.toString() ?: continue
                    val s = b.getCharSequence("sender")?.toString()
                    lines.add(if (s.isNullOrEmpty() || s == title) t else "$s: $t")
                }
            } else {
                val tl = ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                if (tl != null && tl.isNotEmpty()) {
                    for (l in tl) lines.add(l.toString())
                } else if (text.isNotEmpty()) {
                    lines.add(text)
                }
            }
            if (title.isNotEmpty() || lines.isNotEmpty()) g.chats.add(NotifChat(title.ifEmpty { g.label }, lines))
        }
        for (s in summaries) map[s.packageName]?.keys?.add(s.key)
        groups = map.values.filter { it.chats.isNotEmpty() }
        fire()
    }
}

object Media {
    /** The music session that is playing right now, or null. Needs notification access. */
    fun playing(ctx: Context): MediaController? {
        if (!BubbleNotifListener.isEnabled(ctx)) return null
        return try {
            val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val list = msm.getActiveSessions(ComponentName(ctx, BubbleNotifListener::class.java))
            list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        } catch (e: Exception) {
            null
        }
    }
}

// ===== RadialMenuView =====
/**
 * Full-screen overlay that draws the smoky-glass 300-degree rings.
 * Gesture mode: the touch stream comes from the bubble (thumb never lifts) via thumbRaw()/release().
 * Tap mode: the window is touchable and taps are handled in onTouchEvent().
 */
@SuppressLint("ViewConstructor")
class RadialMenuView(
    ctx: Context,
    private val sfx: SoundFx,
    private val hap: Haptics,
    private val rootItems: () -> List<Item>,
    private val customItems: () -> List<Item>,
    private val notifItems: () -> List<Item>,
    private val listener: Listener
) : View(ctx) {

    interface Listener {
        fun onLaunch(app: AppEntry)
        fun onOpenNotif(g: AppNotifs)
        fun onClosed()
        fun onWantTouch()
    }

    enum class Kind { FOLDER, CAT, APP, MORE, PREV, EMPTY, NOTIF, CLEAR_ALL }

    class Item(
        val kind: Kind,
        val label: String,
        val emoji: String = "",
        val app: AppEntry? = null,
        val apps: (() -> List<AppEntry>)? = null,
        val notif: AppNotifs? = null,
        val icon: Drawable? = null
    )

    private class Frame(
        var items: List<Item>, val title: String, val radius: Float, val labels: Boolean,
        val root: Boolean, val music: Boolean, val notif: Boolean
    ) {
        var page = 0
        var np = 1
        var vis: List<Item> = emptyList()
        var cx = 0f
        var cy = 0f
        var gap = 0f
        var backX = 0f
        var backY = 0f
    }

    private class Ring(
        val f: Frame, val vis: List<Item>, val cx: Float, val cy: Float, val radius: Float,
        val base: Float, val step: Float, val backX: Float, val backY: Float,
        val labels: Boolean, val root: Boolean, val np: Int, val page: Int, val music: Boolean,
        val ox: Float, val oy: Float, val born: Long
    ) {
        var dying = 0L
        var dieX = 0f
        var dieY = 0f
        var launch = -1
        val hl = FloatArray(vis.size)
    }

    private val d = resources.displayMetrics.density
    private val r1 = 120 * d
    private val r2 = 104 * d
    private val rMusic = 135 * d
    private val rNotif = 140 * d
    private val maxItems = 9

    private val stack = ArrayList<Frame>()
    private val rings = ArrayList<Ring>()
    private var cur: Ring? = null
    private var hi = -1
    private var left = false
    private var armed = false
    private var armX = 0f
    private var armY = 0f
    private var tx = 0f
    private var ty = 0f
    private var homeX = 0f
    private var homeY = 0f
    private var pending = false
    private var started = false
    private var closing = false
    private var dwell: Runnable? = null
    private val loc = IntArray(2)

    private var tapMode = false
    private var notifMode = false
    private var music: MediaController? = null
    private var musicHi = -1
    private var cardItem: Item? = null
    private var cardHi = -1
    private val cardRect = RectF()
    private val openRect = RectF()
    private val clearRect = RectF()
    private var flipLockX = -100000f
    private var flipLockY = -100000f
    private var downX = 0f
    private var downY = 0f
    private var theme = Theme.GLASS
    private val bot = BotPainter(d)
    private var chipVisible = false
    private var chipHi = false
    private val chipRect = RectF()

    // ---------- paints ----------
    private val aa = Paint.ANTI_ALIAS_FLAG
    private val tint = Paint(aa)
    private val glass = Paint(aa)
    private val spec = Paint(aa)
    private val solid = Paint(aa)
    private val artP = Paint(aa or Paint.FILTER_BITMAP_FLAG)
    private val edge = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * d }
    private val rimThin = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * d; strokeCap = Paint.Cap.ROUND }
    private val prog = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 5f * d; strokeCap = Paint.Cap.ROUND }
    private val bandStroke = Paint(aa).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val lineP = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 2.4f * d; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val emojiP = Paint(aa).apply { textAlign = Paint.Align.CENTER }
    private val labelP = Paint(aa).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setShadowLayer(4f * d, 0f, 1f * d, Color.argb(170, 0, 0, 0))
    }
    private val titleP = TextPaint(aa).apply { textAlign = Paint.Align.CENTER; color = Color.WHITE; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val subP = TextPaint(aa).apply { textAlign = Paint.Align.CENTER; color = Color.WHITE }
    private val headL = TextPaint(aa).apply { textAlign = Paint.Align.LEFT; color = Color.WHITE }
    private val boldL = TextPaint(aa).apply { textAlign = Paint.Align.LEFT; color = Color.WHITE; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val bodyL = TextPaint(aa).apply { textAlign = Paint.Align.LEFT; color = Color.WHITE }
    private val btnP = TextPaint(aa).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val shadowP = Paint(aa)
    private val inner = Paint(aa).apply { style = Paint.Style.STROKE }
    private val iconP = Paint(aa).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val iconFill = Paint(aa)
    private val inkP = TextPaint(aa).apply { textAlign = Paint.Align.CENTER; color = Color.rgb(70, 45, 28); typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    private val woodP = Paint(aa).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val arcPath = Path()
    private val bandPath = Path()
    private val clip = Path()
    private val rect = RectF()

    // ---------- public API (called by the service) ----------

    fun openRaw(rawX: Float, rawY: Float, homeRawX: Float, homeRawY: Float, notif: Boolean, tap: Boolean, mc: MediaController?, th: Int) {
        tx = rawX; ty = rawY; homeX = homeRawX; homeY = homeRawY
        theme = th
        notifMode = notif
        tapMode = tap
        music = if (notif) null else mc
        pending = true
        if (width > 0) startPending()
    }

    fun thumbRaw(rawX: Float, rawY: Float) {
        if (closing || tapMode) return
        if (!started) { tx = rawX; ty = rawY; return }
        getLocationOnScreen(loc)
        thumb(rawX - loc[0], rawY - loc[1])
    }

    /** Thumb lifted at the end of a hold-and-swipe gesture. */
    fun release() {
        if (closing || tapMode) return
        cancelDwell()
        val r = cur
        if (r == null) { close(); return }
        if (musicHi >= 0) {
            musicAction(musicHi)
            toTap()
            return
        }
        if (chipHi) {
            openNotifRing(r)
            toTap()
            return
        }
        val ci = cardItem
        val cg = ci?.notif
        if (r.f.notif && cg != null && cardHi >= 0) {
            if (cardHi == 0) openNotif(r, cg) else { clearNotif(r, cg); toTap() }
            return
        }
        val item = if (hi >= 0 && hi < r.vis.size) r.vis[hi] else null
        if (item == null) { close(); return }
        when (item.kind) {
            Kind.APP -> { val app = item.app; if (app != null) launchApp(r, app) else close() }
            Kind.NOTIF -> { val g = item.notif; if (g != null) openNotif(r, g) else close() }
            Kind.CLEAR_ALL -> { BubbleNotifListener.clearAll(); close() }
            else -> close()
        }
    }

    /** Close from outside (e.g. the bubble was tapped again). */
    fun closeFromOutside() {
        if (!closing) close()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (pending && w > 0) post { if (pending) startPending() }
    }

    private fun startPending() {
        pending = false
        started = true
        getLocationOnScreen(loc)
        tx -= loc[0]; ty -= loc[1]; homeX -= loc[0]; homeY -= loc[1]
        stack.clear()
        val px = if (tapMode) homeX else tx
        val py = if (tapMode) homeY else ty
        val items = if (notifMode) notifItems() else rootItems()
        val rad = when {
            notifMode -> rNotif
            music != null -> rMusic
            else -> r1
        }
        pushRing(items, px, py, null, null, rad, true, if (notifMode) "Notifications" else "", music != null, notifMode)
    }

    // ---------- ring logic ----------

    private fun now() = SystemClock.uptimeMillis()
    private fun deg(r: Float) = r * 57.29578f
    private fun rad(dg: Float) = dg * 0.017453292f
    private fun clampF(v: Float, lo: Float, hi: Float) = if (hi < lo) (lo + hi) / 2f else v.coerceIn(lo, hi)
    private fun argbW(a: Float) = Color.argb(a.toInt().coerceIn(0, 255), 255, 255, 255)
    private fun ang(r: Ring, i: Int) = rad(r.base + r.step * (i + 0.5f))
    private fun ellip(s: String, p: TextPaint, w: Float) = TextUtils.ellipsize(s, p, w, TextUtils.TruncateAt.END).toString()

    private fun pushRing(
        items: List<Item>, px: Float, py: Float, fromX: Float?, fromY: Float?, radius: Float,
        labels: Boolean, title: String, musicF: Boolean, notifF: Boolean
    ) {
        val f = Frame(items, title, radius, labels, stack.isEmpty(), musicF, notifF)
        val m = radius + 46 * d
        f.cx = clampF(px, m, width - m)
        f.cy = clampF(py, m, height - m)
        val dist = hypot(px - f.cx, py - f.cy)
        if (dist > 30 * d) {
            // Ring was pushed away from a screen edge: the gap (and back button) sits right under the thumb.
            f.gap = deg(atan2(py - f.cy, px - f.cx))
            f.backX = px; f.backY = py
        } else {
            f.gap = if (fromX != null && fromY != null) deg(atan2(fromY - f.cy, fromX - f.cx)) else 90f
            f.backX = f.cx; f.backY = f.cy
            if (musicF || notifF || tapMode) {
                // keep the centre free for the music player / message card
                val a = rad(f.gap)
                f.backX = f.cx + radius * cos(a); f.backY = f.cy + radius * sin(a)
            }
        }
        stack.add(f)
        render(f, px, py)
    }

    private fun render(f: Frame, ox: Float, oy: Float) {
        hi = -1; left = false; armed = false; armX = tx; armY = ty; musicHi = -1; cardHi = -1; chipHi = false
        if (!f.notif) cardItem = null
        cancelDwell()
        val n = f.items.size
        if (n > maxItems) {
            val per = maxItems - 2
            f.np = (n + per - 1) / per
            f.page = f.page.coerceIn(0, f.np - 1)
            val s = f.page * per
            val list = ArrayList<Item>()
            if (f.page > 0) list.add(Item(Kind.PREV, "Previous", "prev"))
            list.addAll(f.items.subList(s, min(n, s + per)))
            if (f.page < f.np - 1) list.add(Item(Kind.MORE, "Next", "next"))
            f.vis = list
        } else {
            f.np = 1; f.page = 0; f.vis = f.items
        }
        val step = 300f / f.vis.size
        val r = Ring(f, f.vis, f.cx, f.cy, f.radius, f.gap + 30f, step, f.backX, f.backY,
            f.labels, f.root, f.np, f.page, f.music && music != null, ox, oy, now())
        rings.add(r)
        cur = r
        invalidate()
    }

    private fun dismissCurrent(x: Float, y: Float, launch: Int) {
        val r = cur ?: return
        r.dying = now(); r.dieX = x; r.dieY = y; r.launch = launch
        cur = null
        invalidate()
    }

    private fun pick(r: Ring, x: Float, y: Float): Int {
        val dx = x - r.cx
        val dy = y - r.cy
        if (hypot(dx, dy) < 40 * d || hypot(x - r.backX, y - r.backY) < 30 * d) return -1
        var rel = (deg(atan2(dy, dx)) - r.base) % 360f
        if (rel < 0) rel += 360f
        if (rel >= 300f) return -1
        return min(r.vis.size - 1, (rel / r.step).toInt())
    }

    private fun pickTap(r: Ring, x: Float, y: Float): Int {
        for (i in r.vis.indices) {
            val a = ang(r, i)
            if (hypot(x - (r.cx + r.radius * cos(a)), y - (r.cy + r.radius * sin(a))) < 30 * d) return i
        }
        return -1
    }

    private fun musicRadius(r: Ring) = r.radius - 40 * d
    private fun inMusic(r: Ring, x: Float, y: Float) = r.music && music != null && hypot(x - r.cx, y - r.cy) < musicRadius(r)
    private fun ctrlX(r: Ring, i: Int) = r.cx + (i - 1) * 44 * d
    private fun ctrlY(r: Ring) = r.cy + musicRadius(r) * 0.5f

    private fun musicControlAt(r: Ring, x: Float, y: Float): Int {
        for (i in 0..2) if (hypot(x - ctrlX(r, i), y - ctrlY(r)) < 22 * d) return i
        return -1
    }

    private fun thumb(x: Float, y: Float) {
        tx = x; ty = y
        val r = cur ?: return
        if (!armed && hypot(x - armX, y - armY) > 30 * d) armed = true

        // music player in the centre
        if (inMusic(r, x, y)) {
            val mh = musicControlAt(r, x, y)
            if (mh != musicHi) { musicHi = mh; if (mh >= 0) { sfx.play("tick"); hap.tick() }; invalidate() }
            if (hi != -1) { hi = -1; cancelDwell(); invalidate() }
            return
        } else if (musicHi != -1) {
            musicHi = -1; invalidate()
        }

        // "new notifications" chip above the companion
        if (chipVisible && chipRect.contains(x, y)) {
            if (!chipHi) { chipHi = true; sfx.play("tick"); hap.tick(); invalidate() }
            return
        } else if (chipHi) {
            chipHi = false; invalidate()
        }

        // message card in the centre
        if (r.f.notif && cardItem != null && cardRect.contains(x, y)) {
            val ch = if (openRect.contains(x, y)) 0 else if (clearRect.contains(x, y)) 1 else -1
            if (ch != cardHi) { cardHi = ch; if (ch >= 0) { sfx.play("tick"); hap.tick() }; invalidate() }
            return
        } else if (cardHi != -1) {
            cardHi = -1; invalidate()
        }

        val dB = hypot(x - r.backX, y - r.backY)
        if (dB > 45 * d) left = true
        if (!r.root && left && dB < 24 * d) { pop(); return }
        val p = pick(r, x, y)
        if (p != hi) {
            hi = p
            cancelDwell()
            if (p >= 0) {
                sfx.play("tick"); hap.tick()
                val it2 = r.vis[p]
                if (it2.kind == Kind.NOTIF) cardItem = it2
            }
            invalidate()
        }
        if (hi < 0) return
        val item = r.vis[hi]
        when (item.kind) {
            Kind.MORE, Kind.PREV -> if (dwell == null && hypot(x - flipLockX, y - flipLockY) > 36 * d) {
                val dir = if (item.kind == Kind.MORE) 1 else -1
                val run = Runnable { dwell = null; flip(r.f, dir) }
                dwell = run
                postDelayed(run, 550)
            }
            Kind.CAT, Kind.FOLDER -> if (armed) {
                val a = ang(r, hi)
                val bx = r.cx + r.radius * cos(a)
                val by = r.cy + r.radius * sin(a)
                if (hypot(x - bx, y - by) < 30 * d || hypot(x - r.cx, y - r.cy) > r.radius + 14 * d) {
                    openChild(r, item, x, y)
                }
            }
            else -> {}
        }
    }

    /** One page per hold. To flip again, slide off the arrow and back on. */
    private fun flip(f: Frame, dir: Int) {
        if (closing || cur?.f !== f) return
        sfx.play("page"); hap.click()
        flipLockX = tx; flipLockY = ty
        dismissCurrent(f.cx, f.cy, -1)
        f.page = (f.page + dir).coerceIn(0, max(0, f.np - 1))
        render(f, f.cx, f.cy)
        armed = true
    }

    private fun openChild(r: Ring, item: Item, x: Float, y: Float) {
        sfx.play("sub"); hap.click()
        val folder = item.kind == Kind.FOLDER
        val items = if (folder) customItems() else appItems(item.apps?.invoke() ?: emptyList())
        val rad = if (folder) r1 else r2
        if (tapMode) {
            dismissCurrent(r.cx, r.cy, -1)
            pushRing(items, r.cx, r.cy, homeX, homeY, rad, folder, item.label, false, false)
        } else {
            dismissCurrent(x, y, -1)
            pushRing(items, x, y, r.cx, r.cy, rad, folder, item.label, false, false)
        }
    }

    private fun pop() {
        val r = cur ?: return
        cancelDwell()
        sfx.play("close"); hap.tick()
        dismissCurrent(r.backX, r.backY, -1)
        if (stack.size > 1) stack.removeAt(stack.size - 1)
        render(stack[stack.size - 1], r.backX, r.backY)
    }

    private fun appItems(list: List<AppEntry>): List<Item> =
        if (list.isEmpty()) listOf(Item(Kind.EMPTY, "Empty", "plus"))
        else list.map { Item(Kind.APP, it.label, app = it) }

    private fun finishLater() {
        postDelayed({ listener.onClosed() }, 460)
    }

    private fun close() {
        closing = true
        cancelDwell()
        sfx.play("close"); hap.tick()
        cardItem = null
        dismissCurrent(homeX, homeY, -1)
        finishLater()
    }

    private fun launchApp(r: Ring, app: AppEntry) {
        closing = true
        cancelDwell()
        sfx.play("launch"); hap.heavy()
        dismissCurrent(r.cx, r.cy, hi)
        listener.onLaunch(app)
        finishLater()
    }

    private fun openNotif(r: Ring, g: AppNotifs) {
        closing = true
        cancelDwell()
        sfx.play("launch"); hap.heavy()
        cardItem = null
        dismissCurrent(r.cx, r.cy, hi)
        listener.onOpenNotif(g)
        finishLater()
    }

    private fun clearNotif(r: Ring, g: AppNotifs) {
        BubbleNotifListener.clearPkg(g.pkg)
        sfx.play("close"); hap.click()
        cardItem = null
        val f = r.f
        f.items = f.items.filter { it.notif?.pkg != g.pkg }
        if (f.items.none { it.kind == Kind.NOTIF }) { close(); return }
        dismissCurrent(f.cx, f.cy, -1)
        render(f, f.cx, f.cy)
    }

    private fun musicAction(i: Int) {
        val mc = music ?: return
        try {
            val tc = mc.transportControls
            when (i) {
                0 -> tc.skipToPrevious()
                1 -> if (mc.playbackState?.state == PlaybackState.STATE_PLAYING) tc.pause() else tc.play()
                2 -> tc.skipToNext()
            }
        } catch (e: Exception) { }
        sfx.play("sub"); hap.click()
        invalidate()
    }

    /** Swap the category ring for the notifications ring (from the "new" chip). */
    private fun openNotifRing(r: Ring) {
        sfx.play("sub"); hap.click()
        chipHi = false
        chipVisible = false
        dismissCurrent(r.cx, r.cy, -1)
        stack.clear()
        notifMode = true
        music = null
        pushRing(notifItems(), homeX, homeY, null, null, rNotif, true, "Notifications", false, true)
    }

    private fun toTap() {
        if (!tapMode) {
            tapMode = true
            listener.onWantTouch()
        }
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!tapMode || closing) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y }
            MotionEvent.ACTION_UP -> if (hypot(e.x - downX, e.y - downY) < 16 * d) tap(e.x, e.y)
        }
        return true
    }

    private fun tap(x: Float, y: Float) {
        val r = cur ?: run { close(); return }
        if (inMusic(r, x, y)) {
            val mh = musicControlAt(r, x, y)
            if (mh >= 0) {
                musicHi = mh
                musicAction(mh)
                postDelayed({ musicHi = -1; invalidate() }, 200)
            }
            return
        }
        if (chipVisible && chipRect.contains(x, y)) { openNotifRing(r); return }
        val ci = cardItem
        val cg = ci?.notif
        if (r.f.notif && cg != null && cardRect.contains(x, y)) {
            if (openRect.contains(x, y)) openNotif(r, cg) else if (clearRect.contains(x, y)) clearNotif(r, cg)
            return
        }
        if (hypot(x - r.backX, y - r.backY) < 26 * d) {
            if (r.root) close() else pop()
            return
        }
        val p = pickTap(r, x, y)
        if (p < 0) {
            if (hypot(x - r.cx, y - r.cy) > r.radius + 50 * d) close()
            return
        }
        val item = r.vis[p]
        hi = p
        invalidate()
        when (item.kind) {
            Kind.CAT, Kind.FOLDER -> openChild(r, item, x, y)
            Kind.APP -> { val app = item.app; if (app != null) launchApp(r, app) }
            Kind.MORE -> flip(r.f, 1)
            Kind.PREV -> flip(r.f, -1)
            Kind.NOTIF -> { cardItem = item; sfx.play("tick"); hap.tick() }
            Kind.CLEAR_ALL -> { BubbleNotifListener.clearAll(); close() }
            Kind.EMPTY -> {}
        }
    }

    private fun cancelDwell() {
        dwell?.let { removeCallbacks(it) }
        dwell = null
    }

    override fun onDetachedFromWindow() {
        cancelDwell()
        super.onDetachedFromWindow()
    }

    // ---------- drawing ----------

    private fun backOut(t: Float): Float {
        val c1 = 1.5f
        val c3 = c1 + 1f
        val u = t - 1f
        return 1f + c3 * u * u * u + c1 * u * u
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val t = now()
        var anim = false
        val itr = rings.iterator()
        while (itr.hasNext()) {
            val r = itr.next()
            if (r.dying > 0 && t - r.dying > 420) itr.remove()
        }
        for (r in rings) if (drawRing(c, r, t)) anim = true
        val r = cur
        chipVisible = false
        if (r != null && r.f.notif && cardItem != null) drawCard(c, r)
        else if (r != null && !r.music && hypot(r.backX - r.cx, r.backY - r.cy) > 30 * d) { drawCompanion(c, r, t); anim = true }
        if (anim) postInvalidateOnAnimation()
    }

    private fun drawRing(c: Canvas, r: Ring, t: Long): Boolean {
        var anim = false
        val age = (t - r.born).toFloat()
        val dieT = if (r.dying > 0) ((t - r.dying) / 300f).coerceIn(0f, 1f) else 0f
        if (r.dying > 0) anim = true
        val dieE = dieT * dieT

        val trayT = (age / 420f).coerceIn(0f, 1f)
        if (trayT < 1f) anim = true
        val trayS = (0.3f + 0.7f * backOut(trayT)) * (1f - 0.6f * dieE)
        val trayA = min(1f, age / 180f) * (1f - dieT)
        drawTray(c, r, trayS, trayA, t)
        if (r.music) { drawMusic(c, r, trayS, trayA); anim = true }

        for (i in r.vis.indices) {
            val target = if (r === cur && i == hi) 1f else 0f
            val h = r.hl[i]
            val nh = h + (target - h) * 0.35f
            if (abs(target - nh) < 0.01f) r.hl[i] = target else { r.hl[i] = nh; anim = true }
        }

        for (i in r.vis.indices) {
            val ti = ((age - 24f * i) / 320f).coerceIn(0f, 1f)
            if (ti < 1f) anim = true
            val e = backOut(ti)
            val h = r.hl[i]
            val a0 = ang(r, i)
            val rr = r.radius + (if (r.labels) 0f else 34f * d * h)
            val txp = r.cx + rr * cos(a0)
            val typ = r.cy + rr * sin(a0)
            var x = r.ox + (txp - r.ox) * e
            var y = r.oy + (typ - r.oy) * e
            var sc = (0.3f + 0.7f * e) * (1f + (if (r.labels) 0.2f else 0.5f) * h)
            var a = min(1f, ti * 2.5f)
            if (r.dying > 0) {
                if (i == r.launch) {
                    sc *= 1f + 2.2f * dieT
                    a *= 1f - dieT
                } else {
                    x += (r.dieX - x) * dieE
                    y += (r.dieY - y) * dieE
                    sc *= 1f - 0.7f * dieE
                    a *= 1f - dieT
                }
            }
            drawItem(c, r, r.vis[i], x, y, sc, a, h)
        }

        val bt = (age / 250f).coerceIn(0f, 1f)
        if (bt < 1f) anim = true
        drawBack(c, r, (0.4f + 0.6f * backOut(bt)) * (1f - 0.6f * dieE), bt * (1f - dieT))
        return anim
    }

    /**
     * Liquid glass: a clear, slightly bright body, a top-down sheen, a rim that catches light at the
     * top-left and bottom-right, a thin inner edge for thickness and a soft floating shadow.
     * Ninja theme swaps the glass for parchment.
     */
    private fun glassPath(c: Canvas, p: Path, top: Float, bottom: Float, a: Float, h: Float, gx: Float, gy: Float, forceGlass: Boolean = false) {
        if (a <= 0.01f) return
        if (theme == Theme.NINJA && !forceGlass) { paperPath(c, p, top, bottom, a, h); return }
        shadowP.color = Color.argb((18 * a).toInt().coerceIn(0, 255), 0, 0, 0)
        shadowP.setShadowLayer(12 * d, 0f, 5 * d, Color.argb((60 * a).toInt().coerceIn(0, 255), 0, 0, 0))
        c.drawPath(p, shadowP)
        shadowP.clearShadowLayer()
        tint.color = if (theme == Theme.OCEAN) Color.argb(((38 + 30 * h) * a).toInt().coerceIn(0, 255), 120, 200, 255)
            else Color.argb(((30 + 34 * h) * a).toInt().coerceIn(0, 255), 255, 255, 255)
        c.drawPath(p, tint)
        glass.shader = LinearGradient(0f, top, 0f, bottom,
            intArrayOf(argbW((70 + 40 * h) * a), argbW(6 * a), argbW((26 + 20 * h) * a)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(p, glass)
        glass.shader = null
        val hv = (235 + 20 * h) * a
        edge.shader = SweepGradient(gx, gy,
            intArrayOf(argbW(80 * a), argbW(hv), argbW(40 * a), argbW(60 * a), argbW(hv), argbW(40 * a), argbW(80 * a)),
            floatArrayOf(0f, 0.125f, 0.3f, 0.5f, 0.625f, 0.8f, 1f))
        edge.strokeWidth = 1.3f * d
        c.drawPath(p, edge)
        edge.shader = null
        inner.strokeWidth = 3.5f * d
        inner.shader = LinearGradient(0f, top, 0f, bottom, argbW(0f), argbW(55 * a), Shader.TileMode.CLAMP)
        c.save()
        c.clipPath(p)
        c.drawPath(p, inner)
        c.restore()
        inner.shader = null
    }

    /** Ninja scroll parchment. */
    private fun paperPath(c: Canvas, p: Path, top: Float, bottom: Float, a: Float, h: Float) {
        shadowP.color = Color.argb((30 * a).toInt().coerceIn(0, 255), 0, 0, 0)
        shadowP.setShadowLayer(10 * d, 0f, 4 * d, Color.argb((90 * a).toInt().coerceIn(0, 255), 40, 20, 0))
        c.drawPath(p, shadowP)
        shadowP.clearShadowLayer()
        val al = (245 * a).toInt().coerceIn(0, 255)
        tint.shader = LinearGradient(0f, top, 0f, bottom,
            intArrayOf(Color.argb(al, 244, 232, 204), Color.argb(al, 232, 214, 176), Color.argb(al, 214, 190, 148)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        c.drawPath(p, tint)
        tint.shader = null
        if (h > 0f) { tint.color = Color.argb((60 * h * a).toInt().coerceIn(0, 255), 255, 250, 235); c.drawPath(p, tint) }
        edge.color = if (h > 0.5f) Color.argb(al, 176, 52, 44) else Color.argb((220 * a).toInt().coerceIn(0, 255), 110, 74, 42)
        edge.strokeWidth = (1.4f + h) * d
        c.drawPath(p, edge)
    }

    private fun drawTray(c: Canvas, r: Ring, s: Float, a: Float, t: Long) {
        if (a <= 0.01f) return
        val rIn = r.radius - 31 * d
        val rOut = r.radius + 38 * d
        val mid = (rIn + rOut) / 2f
        val w = rOut - rIn
        val ninja = theme == Theme.NINJA
        val cap = if (ninja) 0f else deg(w / 2f / mid)
        val sweep = 300f - 2f * cap
        c.save()
        c.scale(s, s, r.cx, r.cy)
        rect.set(r.cx - mid, r.cy - mid, r.cx + mid, r.cy + mid)
        arcPath.reset()
        arcPath.addArc(rect, r.base + cap, sweep)
        bandStroke.strokeWidth = w
        bandStroke.strokeCap = if (ninja) Paint.Cap.BUTT else Paint.Cap.ROUND
        bandPath.reset()
        @Suppress("DEPRECATION")
        bandStroke.getFillPath(arcPath, bandPath)
        glassPath(c, bandPath, r.cy - rOut, r.cy + rOut, a, 0f, r.cx, r.cy)
        if (ninja) {
            // red border lines and wooden rollers at both ends of the scroll
            rimThin.color = Color.argb((200 * a).toInt().coerceIn(0, 255), 170, 50, 44)
            rimThin.strokeWidth = 1.3f * d
            for (rr in floatArrayOf(rIn + 6 * d, rOut - 6 * d)) {
                rect.set(r.cx - rr, r.cy - rr, r.cx + rr, r.cy + rr)
                c.drawArc(rect, r.base + 2f, sweep - 4f, false, rimThin)
            }
            rimThin.strokeWidth = 1.5f * d
            for (endA in floatArrayOf(r.base, r.base + 300f)) {
                val ar = rad(endA)
                val x1 = r.cx + (rIn - 9 * d) * cos(ar)
                val y1 = r.cy + (rIn - 9 * d) * sin(ar)
                val x2 = r.cx + (rOut + 9 * d) * cos(ar)
                val y2 = r.cy + (rOut + 9 * d) * sin(ar)
                woodP.strokeWidth = 13 * d
                woodP.shader = LinearGradient(x1 - 6 * d * sin(ar), y1 + 6 * d * cos(ar), x1 + 6 * d * sin(ar), y1 - 6 * d * cos(ar),
                    intArrayOf(Color.argb((255 * a).toInt().coerceIn(0, 255), 120, 72, 36), Color.argb((255 * a).toInt().coerceIn(0, 255), 196, 136, 80), Color.argb((255 * a).toInt().coerceIn(0, 255), 96, 56, 28)),
                    floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
                c.drawLine(x1, y1, x2, y2, woodP)
                woodP.shader = null
                solid.color = Color.argb((255 * a).toInt().coerceIn(0, 255), 70, 40, 20)
                c.drawCircle(x1, y1, 7.5f * d, solid)
                c.drawCircle(x2, y2, 7.5f * d, solid)
                solid.color = Color.argb((120 * a).toInt().coerceIn(0, 255), 255, 220, 170)
                c.drawCircle(x1 - 2 * d, y1 - 2 * d, 2.2f * d, solid)
                c.drawCircle(x2 - 2 * d, y2 - 2 * d, 2.2f * d, solid)
            }
        } else {
            if (theme == Theme.OCEAN) {
                // tiny bubbles rising through the glass
                val ts = (t % 600000L) / 1000f
                rimThin.strokeWidth = 1.2f * d
                for (k in 0 until 9) {
                    val ph = (ts * 0.25f + k * 0.37f) % 1f
                    val ang = rad(r.base + 8f + k * 284f / 8f + sin(ph * 6f + k) * 2f)
                    val rr = rOut - 6 * d - ph * (w - 12 * d)
                    rimThin.color = argbW(150 * a * (1f - ph))
                    c.drawCircle(r.cx + rr * cos(ang), r.cy + rr * sin(ang), (1.6f + (k % 3)) * d, rimThin)
                }
            }
            val ri = rIn + 2.5f * d
            rect.set(r.cx - ri, r.cy - ri, r.cx + ri, r.cy + ri)
            rimThin.strokeWidth = 1.2f * d
            rimThin.color = argbW(55 * a)
            c.drawArc(rect, r.base + cap, sweep, false, rimThin)
        }
        c.restore()
    }

    private fun glassTile(c: Canvas, x: Float, y: Float, half: Float, a: Float, h: Float) {
        val cr = half * 0.44f
        rect.set(x - half, y - half, x + half, y + half)
        clip.reset()
        clip.addRoundRect(rect, cr, cr, Path.Direction.CW)
        glassPath(c, clip, y - half, y + half, a, h, x, y)
    }

    private fun inkColor(a: Float) = if (theme == Theme.NINJA) Color.argb((255 * a).toInt().coerceIn(0, 255), 58, 38, 22) else argbW(255 * a)

    private fun drawItem(c: Canvas, r: Ring, item: Item, x: Float, y: Float, sc: Float, a: Float, h: Float) {
        if (a <= 0.01f) return
        val half = 23 * d * sc
        glassTile(c, x, y, half, a, h)
        val ic = item.app?.icon ?: item.icon
        if (ic != null) {
            val k = half - 5 * d * sc
            ic.setBounds((x - k).toInt(), (y - k).toInt(), (x + k).toInt(), (y + k).toInt())
            ic.alpha = (255 * a).toInt().coerceIn(0, 255)
            ic.draw(c)
        } else {
            iconP.color = inkColor(a)
            iconFill.color = inkColor(a)
            Icons.draw(c, item.emoji, x, y, 24 * d * sc, iconP, iconFill)
        }
        // unread count
        val g = item.notif
        if (g != null && g.count > 0) {
            val bx = x + half * 0.8f
            val by = y - half * 0.8f
            solid.color = Color.argb((255 * a).toInt().coerceIn(0, 255), 255, 59, 48)
            c.drawCircle(bx, by, 8 * d * sc, solid)
            btnP.color = Color.WHITE
            btnP.alpha = (255 * a).toInt().coerceIn(0, 255)
            btnP.textSize = 10 * d * sc
            val fm = btnP.fontMetrics
            c.drawText(if (g.count > 9) "9+" else g.count.toString(), bx, by - (fm.ascent + fm.descent) / 2f, btnP)
        }
        if (r.labels || h > 0.05f) {
            var label = when {
                item.kind == Kind.MORE && h > 0.05f && !tapMode -> "Hold for next page"
                item.kind == Kind.PREV && h > 0.05f && !tapMode -> "Hold for previous"
                else -> item.label
            }
            if (label.isEmpty()) return
            if (label.length > 18) label = label.take(17) + "…"
            val tp: Paint = if (theme == Theme.NINJA && r.labels) inkP else labelP
            tp.textSize = 12f * d * (if (r.labels) 1f else 1.1f)
            tp.alpha = (255 * a * (if (r.labels) 1f else h)).toInt().coerceIn(0, 255)
            c.drawText(label, x, y + half + 15 * d, tp)
        }
    }

    private fun drawBack(c: Canvas, r: Ring, s: Float, a: Float) {
        if (a <= 0.01f) return
        val x = r.backX
        val y = r.backY
        val rad = 22 * d * s
        clip.reset()
        clip.addCircle(x, y, rad, Path.Direction.CW)
        glassPath(c, clip, y - rad, y + rad, a, 0f, x, y)
        lineP.color = inkColor(a * 0.95f)
        val k = 6 * d * s
        if (r.root) {
            c.drawLine(x - k, y - k, x + k, y + k, lineP)
            c.drawLine(x - k, y + k, x + k, y - k, lineP)
        } else {
            arcPath.reset()
            arcPath.moveTo(x + k * 0.5f, y - k)
            arcPath.lineTo(x - k * 0.6f, y)
            arcPath.lineTo(x + k * 0.5f, y + k)
            c.drawPath(arcPath, lineP)
        }
        if (r.np > 1) {
            val gapD = 9 * d
            val start = x - (r.np - 1) * gapD / 2f
            for (i in 0 until r.np) {
                solid.color = argbW((if (i == r.page) 255f else 100f) * a)
                c.drawCircle(start + i * gapD, y + rad + 10 * d, 2.7f * d, solid)
            }
        }
    }

    /** Glass pill with centred text. */
    private fun pill(c: Canvas, text: String, cx: Float, cy: Float, a: Float, h: Float, out: RectF?) {
        btnP.textSize = 12.5f * d
        val tw = btnP.measureText(text)
        rect.set(cx - tw / 2f - 12 * d, cy - 12 * d, cx + tw / 2f + 12 * d, cy + 12 * d)
        out?.set(rect)
        val l = rect.left; val t = rect.top; val rr = rect.right; val b = rect.bottom
        clip.reset()
        clip.addRoundRect(rect, 12 * d, 12 * d, Path.Direction.CW)
        glassPath(c, clip, t, b, a, h, (l + rr) / 2f, (t + b) / 2f)
        btnP.color = inkColor(a)
        val fm = btnP.fontMetrics
        c.drawText(text, cx, cy - (fm.ascent + fm.descent) / 2f, btnP)
    }

    /** The companion bot in the middle of the ring, plus a speech pill and a "new notifications" chip. */
    private fun drawCompanion(c: Canvas, r: Ring, now: Long) {
        val age = (now - r.born).toFloat()
        val e = backOut(((age - 80f) / 380f).coerceIn(0f, 1f))
        if (e <= 0.02f) return
        val cr = min((r.radius - 31 * d) * 0.55f, 46 * d) * e
        val cy0 = r.cy - 4 * d
        val hov = if (hi >= 0 && hi < r.vis.size) r.vis[hi] else null
        val bias = if (hov != null) cos(ang(r, hi)) else 0f
        val mood = if (hov != null) BotPainter.Mood.HAPPY else BotPainter.Mood.IDLE
        bot.drawSphere(c, r.cx, cy0, cr, mood, now, theme, false, bias)
        val a = min(1f, e)
        val text = when {
            hov != null && hov.kind == Kind.MORE -> "Next page"
            hov != null && hov.kind == Kind.PREV -> "Previous page"
            hov != null -> hov.label
            r.f.notif -> "Pick an app to read"
            r.root -> if (tapMode) "Tap a category" else "Slide to a category"
            else -> r.f.title
        }
        btnP.textSize = 12.5f * d
        pill(c, ellip(text, btnP, 140 * d), r.cx, cy0 + cr + 18 * d, a, 0f, null)
        val n = BubbleNotifListener.groups.size
        if (r.root && !r.f.notif && n > 0) {
            val label = "●  " + n + (if (n == 1) " new" else " new")
            pill(c, label, r.cx, cy0 - cr - 18 * d, a, if (chipHi) 1f else 0f, chipRect)
            // red dot over the bullet
            solid.color = Color.argb((255 * a).toInt().coerceIn(0, 255), 255, 59, 48)
            btnP.textSize = 12.5f * d
            val dotX = chipRect.left + 12 * d + btnP.measureText("●") / 2f
            c.drawCircle(dotX, chipRect.centerY(), 4 * d, solid)
            chipVisible = true
        }
    }

    private fun fmt(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return String.format("%d:%02d", s / 60, s % 60)
    }

    private fun tri(c: Canvas, x: Float, y: Float, size: Float, right: Boolean, p: Paint) {
        arcPath.reset()
        val dir = if (right) 1f else -1f
        arcPath.moveTo(x - dir * size * 0.5f, y - size * 0.6f)
        arcPath.lineTo(x + dir * size * 0.55f, y)
        arcPath.lineTo(x - dir * size * 0.5f, y + size * 0.6f)
        arcPath.close()
        c.drawPath(arcPath, p)
    }

    private fun drawMusic(c: Canvas, r: Ring, s: Float, a: Float) {
        val mc = music ?: return
        if (a <= 0.01f) return
        val md = try { mc.metadata } catch (e: Exception) { null }
        val st = try { mc.playbackState } catch (e: Exception) { null }
        val mr = musicRadius(r)
        c.save()
        c.scale(s, s, r.cx, r.cy)

        clip.reset()
        clip.addCircle(r.cx, r.cy, mr, Path.Direction.CW)
        tint.color = Color.argb((190 * a).toInt().coerceIn(0, 255), 30, 30, 36)
        c.drawPath(clip, tint)
        val art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        if (art != null) {
            c.save()
            c.clipPath(clip)
            rect.set(r.cx - mr, r.cy - mr, r.cx + mr, r.cy + mr)
            artP.alpha = (85 * a).toInt().coerceIn(0, 255)
            c.drawBitmap(art, null, rect, artP)
            c.restore()
        }
        glassPath(c, clip, r.cy - mr, r.cy + mr, a * 0.7f, 0f, r.cx, r.cy, true)

        // progress ring
        val dur = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val playing = st?.state == PlaybackState.STATE_PLAYING
        var pos = st?.position ?: 0L
        if (st != null && playing) pos += ((SystemClock.elapsedRealtime() - st.lastPositionUpdateTime) * st.playbackSpeed).toLong()
        val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
        val pr = mr - 9 * d
        rect.set(r.cx - pr, r.cy - pr, r.cx + pr, r.cy + pr)
        prog.color = argbW(45 * a)
        c.drawArc(rect, 0f, 360f, false, prog)
        prog.color = Color.argb((255 * a).toInt().coerceIn(0, 255), 52, 199, 89)
        if (frac > 0f) c.drawArc(rect, -90f, 360f * frac, false, prog)

        // text
        val tw = mr * 1.45f
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Now playing"
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        titleP.textSize = 15 * d
        titleP.alpha = (255 * a).toInt().coerceIn(0, 255)
        c.drawText(ellip(title, titleP, tw), r.cx, r.cy - mr * 0.28f, titleP)
        subP.textSize = 12 * d
        subP.alpha = (180 * a).toInt().coerceIn(0, 255)
        if (artist.isNotEmpty()) c.drawText(ellip(artist, subP, tw), r.cx, r.cy - mr * 0.28f + 17 * d, subP)
        subP.textSize = 11 * d
        subP.alpha = (150 * a).toInt().coerceIn(0, 255)
        if (dur > 0) c.drawText(fmt(pos) + " / " + fmt(dur), r.cx, r.cy + mr * 0.14f, subP)

        // controls
        val cy = ctrlY(r)
        for (i in 0..2) {
            val x = ctrlX(r, i)
            val hl = musicHi == i
            if (i == 1) {
                solid.color = argbW(255 * a)
                c.drawCircle(x, cy, (if (hl) 22f else 19f) * d, solid)
                solid.color = Color.argb((255 * a).toInt().coerceIn(0, 255), 30, 30, 36)
                if (playing) {
                    rect.set(x - 6 * d, cy - 7 * d, x - 2 * d, cy + 7 * d); c.drawRoundRect(rect, d, d, solid)
                    rect.set(x + 2 * d, cy - 7 * d, x + 6 * d, cy + 7 * d); c.drawRoundRect(rect, d, d, solid)
                } else {
                    tri(c, x + d, cy, 14 * d, true, solid)
                }
            } else {
                if (hl) { solid.color = argbW(60 * a); c.drawCircle(x, cy, 18 * d, solid) }
                solid.color = argbW(235 * a)
                val right = i == 2
                val o = if (right) 1f else -1f
                tri(c, x - o * 4 * d, cy, 10 * d, right, solid)
                tri(c, x + o * 4 * d, cy, 10 * d, right, solid)
            }
        }
        c.restore()
    }

    private fun drawCard(c: Canvas, r: Ring) {
        val g = cardItem?.notif ?: return
        val w = 196 * d
        val h = 164 * d
        cardRect.set(r.cx - w / 2f, r.cy - h / 2f, r.cx + w / 2f, r.cy + h / 2f)
        clip.reset()
        clip.addRoundRect(cardRect, 30 * d, 30 * d, Path.Direction.CW)
        tint.color = Color.argb(120, 10, 10, 16)
        c.drawPath(clip, tint)
        glassPath(c, clip, cardRect.top, cardRect.bottom, 1f, 0f, cardRect.centerX(), cardRect.centerY(), true)

        val left = cardRect.left + 18 * d
        val tw = w - 36 * d
        val by = cardRect.bottom - 16 * d - 32 * d
        var y = cardRect.top + 26 * d
        headL.textSize = 12 * d
        headL.alpha = 170
        val head = g.label + "  ·  " + g.count + (if (g.count == 1) " notification" else " notifications")
        c.drawText(ellip(head, headL, tw), left, y, headL)
        y += 22 * d
        boldL.textSize = 14 * d
        bodyL.textSize = 13 * d
        bodyL.alpha = 225
        loop@ for (chat in g.chats) {
            if (y > by - 8 * d) break
            c.drawText(ellip(chat.title, boldL, tw), left, y, boldL)
            y += 18 * d
            for (ln in chat.lines.takeLast(2)) {
                if (y > by - 8 * d) break@loop
                c.drawText(ellip(ln, bodyL, tw), left, y, bodyL)
                y += 17 * d
            }
            y += 5 * d
        }

        openRect.set(left, by, left + (tw - 10 * d) / 2f, by + 32 * d)
        clearRect.set(openRect.right + 10 * d, by, cardRect.right - 18 * d, by + 32 * d)
        btnP.textSize = 13 * d
        solid.color = if (cardHi == 0) Color.WHITE else Color.argb(230, 255, 255, 255)
        c.drawRoundRect(openRect, 16 * d, 16 * d, solid)
        btnP.color = Color.rgb(30, 30, 36)
        val fm = btnP.fontMetrics
        c.drawText("Open", openRect.centerX(), openRect.centerY() - (fm.ascent + fm.descent) / 2f, btnP)
        solid.color = if (cardHi == 1) Color.argb(230, 255, 59, 48) else Color.argb(60, 255, 255, 255)
        c.drawRoundRect(clearRect, 16 * d, 16 * d, solid)
        btnP.color = Color.WHITE
        c.drawText("Clear", clearRect.centerX(), clearRect.centerY() - (fm.ascent + fm.descent) / 2f, btnP)
    }
}

// ===== BubbleService =====
/**
 * Keeps the robot bubble floating over every app.
 * Tap: tap menu. Double tap: open the Bubble app. Hold: swipe menu. Double tap and hold: notifications.
 */
class BubbleService : Service() {

    private var wm: WindowManager? = null
    private var bubble: BubbleView? = null
    private var bp: WindowManager.LayoutParams? = null
    private var menu: RadialMenuView? = null
    private var menuLp: WindowManager.LayoutParams? = null
    private var sfx: SoundFx? = null
    private var hap: Haptics? = null
    private val h = Handler(Looper.getMainLooper())
    private val dens by lazy { resources.displayMetrics.density }

    private var mode = 0 // 0 idle, 1 pressed, 2 dragging, 3 swipe menu open
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0
    private var startY = 0
    private var receiverOn = false
    private var secondTap = false
    private var pendingTap = false
    private var lastTapUp = 0L

    private val holdRunnable = Runnable { if (mode == 1) { mode = 3; openMenu(secondTap, false) } }
    private val tapRunnable = Runnable { pendingTap = false; openMenu(false, true) }
    private val musicPoll = object : Runnable {
        override fun run() {
            bubble?.musicOn = Media.playing(this@BubbleService) != null
            bubble?.theme = Store.theme(this@BubbleService)
            h.postDelayed(this, 3000)
        }
    }

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = reload()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startFg()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        sfx = SoundFx(this, Store.theme(this))
        hap = Haptics(this)
        reload()
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(pkgReceiver, f, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(pkgReceiver, f)
        receiverOn = true
        addBubble()
        BubbleNotifListener.onChange = { bubble?.badge = BubbleNotifListener.groups.isNotEmpty() }
        bubble?.badge = BubbleNotifListener.groups.isNotEmpty()
        h.post(musicPoll)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("bubble", "Floating bubble", NotificationManager.IMPORTANCE_MIN))
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "bubble")
            .setContentTitle("Bubble is floating")
            .setContentText("Tap to open settings")
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun reload() {
        Thread { AppRepo.load(applicationContext) }.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addBubble() {
        val w = wm ?: return
        val size = (66 * dens).toInt()
        val p = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.TOP or Gravity.START
        val dm = resources.displayMetrics
        val prefs = Store.prefs(this)
        p.x = prefs.getInt("bx", dm.widthPixels - size)
        p.y = prefs.getInt("by", (dm.heightPixels * 0.55f).toInt())
        val v = BubbleView(this)
        v.theme = Store.theme(this)
        v.setOnTouchListener { _, e -> onTouch(e); true }
        w.addView(v, p)
        bubble = v
        bp = p
    }

    private fun onTouch(e: MotionEvent) {
        val p = bp ?: return
        val w = wm ?: return
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val now = SystemClock.uptimeMillis()
                bubble?.touch()
                secondTap = pendingTap && now - lastTapUp < 300
                h.removeCallbacks(tapRunnable)
                pendingTap = false
                mode = 1
                downX = e.rawX; downY = e.rawY; lastX = e.rawX; lastY = e.rawY
                startX = p.x; startY = p.y
                bubble?.pressedLook = true
                h.postDelayed(holdRunnable, 300)
            }
            MotionEvent.ACTION_MOVE -> {
                lastX = e.rawX; lastY = e.rawY
                if (mode == 1 && hypot(e.rawX - downX, e.rawY - downY) > 8 * dens) {
                    h.removeCallbacks(holdRunnable)
                    mode = 2
                    bubble?.pressedLook = false
                    bubble?.dragging = true
                }
                when (mode) {
                    2 -> {
                        p.x = (startX + (e.rawX - downX)).toInt()
                        p.y = (startY + (e.rawY - downY)).toInt()
                        bubble?.let { w.updateViewLayout(it, p) }
                    }
                    3 -> menu?.thumbRaw(e.rawX, e.rawY)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                h.removeCallbacks(holdRunnable)
                bubble?.pressedLook = false
                when (mode) {
                    1 -> if (e.actionMasked == MotionEvent.ACTION_UP) {
                        if (secondTap) {
                            openSettings()
                        } else {
                            pendingTap = true
                            lastTapUp = SystemClock.uptimeMillis()
                            h.postDelayed(tapRunnable, 260)
                        }
                    }
                    2 -> { bubble?.dragging = false; snapToEdge() }
                    3 -> menu?.release()
                }
                secondTap = false
                mode = 0
            }
        }
    }

    private fun openSettings() {
        sfx?.play("sub")
        hap?.click()
        bubble?.flash(BotPainter.Mood.WINK, 900)
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { startActivity(i) } catch (e: Exception) { }
    }

    private fun snapToEdge() {
        val p = bp ?: return
        val w = wm ?: return
        val dm = resources.displayMetrics
        val size = p.width
        val target = if (p.x + size / 2 < dm.widthPixels / 2) 0 else dm.widthPixels - size
        p.y = p.y.coerceIn(0, maxOf(0, dm.heightPixels - size))
        val anim = ValueAnimator.ofInt(p.x, target)
        anim.duration = 350
        anim.interpolator = OvershootInterpolator(1.2f)
        anim.addUpdateListener {
            p.x = it.animatedValue as Int
            val b = bubble
            if (b != null && b.isAttachedToWindow) w.updateViewLayout(b, p)
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                Store.prefs(this@BubbleService).edit().putInt("bx", p.x).putInt("by", p.y).apply()
            }
        })
        anim.start()
        sfx?.play("drop")
    }

    private fun openMenu(notif: Boolean, tap: Boolean) {
        val w = wm ?: return
        val th = Store.theme(this)
        if (sfx?.theme != th) { sfx?.release(); sfx = SoundFx(this, th) }
        bubble?.theme = th
        val s = sfx ?: return
        val hp = hap ?: return
        menu?.let { old -> try { w.removeViewImmediate(old) } catch (e: Exception) { } }
        menu = null
        s.enabled = Store.sound(this)
        hp.enabled = Store.haptics(this)
        s.play("open")
        hp.click()

        val mv = RadialMenuView(this, s, hp, ::rootItems, ::customItems, ::notifItems, object : RadialMenuView.Listener {
            override fun onLaunch(app: AppEntry) = launch(app)
            override fun onOpenNotif(g: AppNotifs) = openNotif(g)
            override fun onClosed() = closeMenu()
            override fun onWantTouch() = makeMenuTouchable()
        })
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        if (!tap) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.dimAmount = 0.15f
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= 31 && Store.blur(this)) {
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            lp.blurBehindRadius = (10 * dens).toInt()
        }
        try {
            w.addView(mv, lp)
        } catch (e: Exception) {
            return
        }
        menu = mv
        menuLp = lp
        val loc = IntArray(2)
        bubble?.getLocationOnScreen(loc)
        val half = (bp?.width ?: 0) / 2f
        val mc = if (notif) null else Media.playing(this)
        mv.openRaw(lastX, lastY, loc[0] + half, loc[1] + half, notif, tap, mc, th)
        bubble?.menuOpen = true
        bubble?.animate()?.alpha(0.25f)?.setDuration(150)?.start()
    }

    private fun makeMenuTouchable() {
        val m = menu ?: return
        val lp = menuLp ?: return
        lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        try { wm?.updateViewLayout(m, lp) } catch (e: Exception) { }
    }

    private fun closeMenu() {
        val m = menu ?: return
        try { wm?.removeViewImmediate(m) } catch (e: Exception) { }
        if (menu === m) { menu = null; menuLp = null }
        bubble?.menuOpen = false
        bubble?.animate()?.alpha(1f)?.setDuration(200)?.start()
    }

    private fun launch(app: AppEntry) {
        bubble?.flash(BotPainter.Mood.WINK, 900)
        val i = packageManager.getLaunchIntentForPackage(app.pkg) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try { startActivity(i) } catch (e: Exception) { }
    }

    private fun openNotif(g: AppNotifs) {
        bubble?.flash(BotPainter.Mood.WINK, 900)
        val pi = g.intent
        if (pi != null) {
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    val o = ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    pi.send(this, 0, null, null, null, null, o.toBundle())
                } else {
                    pi.send()
                }
                return
            } catch (e: Exception) {
                // fall back to opening the app
            }
        }
        val i = packageManager.getLaunchIntentForPackage(g.pkg) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { startActivity(i) } catch (e: Exception) { }
    }

    private fun rootItems(): List<RadialMenuView.Item> {
        val out = ArrayList<RadialMenuView.Item>()
        out.add(RadialMenuView.Item(RadialMenuView.Kind.FOLDER, "Custom", "custom"))
        for (cd in AppRepo.CATEGORIES) {
            val apps = AppRepo.appsIn(cd.name)
            if (apps.isNotEmpty()) out.add(RadialMenuView.Item(RadialMenuView.Kind.CAT, cd.name, cd.emoji, apps = { apps }))
        }
        return out
    }

    private fun customItems(): List<RadialMenuView.Item> {
        val list = Store.customs(this).map { cc ->
            val apps = cc.pkgs.mapNotNull { AppRepo.byPkg(it) }
            RadialMenuView.Item(RadialMenuView.Kind.CAT, cc.name, "folder", apps = { apps })
        }
        return list.ifEmpty { listOf(RadialMenuView.Item(RadialMenuView.Kind.EMPTY, "No categories", "plus")) }
    }

    private fun notifItems(): List<RadialMenuView.Item> {
        val gs = BubbleNotifListener.groups
        if (gs.isEmpty()) {
            val label = if (BubbleNotifListener.isEnabled(this)) "All clear" else "Allow access in app"
            return listOf(RadialMenuView.Item(RadialMenuView.Kind.EMPTY, label, "bell"))
        }
        val out = ArrayList<RadialMenuView.Item>()
        for (g in gs) {
            out.add(RadialMenuView.Item(RadialMenuView.Kind.NOTIF, g.label, "bell", notif = g, icon = AppRepo.byPkg(g.pkg)?.icon))
        }
        out.add(RadialMenuView.Item(RadialMenuView.Kind.CLEAR_ALL, "Clear all", "clear"))
        return out
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        BubbleNotifListener.onChange = null
        val w = wm
        if (w != null) {
            menu?.let { try { w.removeViewImmediate(it) } catch (e: Exception) { } }
            bubble?.let { try { w.removeViewImmediate(it) } catch (e: Exception) { } }
        }
        menu = null
        bubble = null
        if (receiverOn) {
            try { unregisterReceiver(pkgReceiver) } catch (e: Exception) { }
            receiverOn = false
        }
        sfx?.release()
        super.onDestroy()
    }
}

// ===== BootReceiver =====
/** Brings the bubble back after the phone restarts. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Store.enabled(ctx) || !Settings.canDrawOverlays(ctx)) return
        try {
            ctx.startForegroundService(Intent(ctx, BubbleService::class.java))
        } catch (e: Exception) {
            // Some phones block this; the user can reopen the app.
        }
    }
}

// ===== MainActivity =====
/** Settings screen: permission, on/off switch, sounds, and custom categories. */
class MainActivity : Activity() {

    private val d by lazy { resources.displayMetrics.density }
    private lateinit var status: TextView
    private lateinit var permBtn: Button
    private lateinit var mainSwitch: Switch
    private lateinit var customBox: LinearLayout
    private lateinit var notifStatus: TextView
    private lateinit var notifBtn: Button
    private var binding = false
    private var themeButtons: List<Button> = emptyList()

    private val ink = Color.rgb(38, 33, 92)
    private val muted = Color.rgb(95, 94, 90)
    private val accent = Color.rgb(83, 74, 183)

    private fun dp(v: Int) = (v * d).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(32))
            setBackgroundColor(Color.rgb(244, 243, 251))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(244, 243, 251))
            addView(col)
        }
        setContentView(scroll)

        col.addView(text("Bubble", 30f, ink, true))
        col.addView(text("Hold the floating bubble, swipe to a category, swipe to an app, let go to open it.", 15f, muted, false).apply {
            setPadding(0, dp(6), 0, dp(18))
        })

        // ---- setup card ----
        val setup = card()
        status = text("", 15f, ink, true)
        setup.addView(status)
        permBtn = button("Allow \"Display over other apps\"") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        setup.addView(permBtn)
        mainSwitch = switch("Show floating bubble", Store.enabled(this)) { on ->
            if (binding) return@switch
            if (on && !Settings.canDrawOverlays(this)) {
                toast("Allow the permission first")
                refresh()
                return@switch
            }
            Store.setEnabled(this, on)
            if (on) startForegroundService(Intent(this, BubbleService::class.java))
            else stopService(Intent(this, BubbleService::class.java))
        }
        setup.addView(mainSwitch)
        col.addView(setup)

        // ---- feel card ----
        val feel = card()
        feel.addView(text("Feel", 17f, ink, true))
        feel.addView(switch("Sounds", Store.sound(this)) { Store.setSound(this, it) })
        feel.addView(switch("Vibration", Store.haptics(this)) { Store.setHaptics(this, it) })
        feel.addView(switch("Blur background (Android 12+)", Store.blur(this)) { Store.setBlur(this, it) })
        col.addView(feel)

        // ---- theme card ----
        val tc = card()
        tc.addView(text("Theme", 17f, ink, true))
        val themeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(6), 0, 0) }
        tc.addView(themeRow)
        tc.addView(text("Each theme changes the bubble character, the menu tray and the sounds.", 13f, muted, false).apply { setPadding(0, dp(8), 0, 0) })
        col.addView(tc)
        themeButtons = Theme.NAMES.mapIndexed { i, n -> smallButton(n) { Store.setTheme(this, i); paintThemes() }.also { themeRow.addView(it) } }
        paintThemes()

        // ---- notifications and music card ----
        val nc = card()
        nc.addView(text("Notifications and music", 17f, ink, true))
        notifStatus = text("", 14f, muted, false).apply { setPadding(0, dp(4), 0, 0) }
        nc.addView(notifStatus)
        notifBtn = button("Allow notification access") {
            try { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (e: Exception) { toast("Open Settings > Notifications > Device and app notifications") }
        }
        nc.addView(notifBtn)
        nc.addView(text("If Android says \"Restricted setting\", tap the button below, open the \u22EE menu at the top right and choose \"Allow restricted settings\". Then try again.", 13f, muted, false).apply {
            setPadding(0, dp(8), 0, 0)
        })
        nc.addView(smallButton("Open app info") {
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) } catch (e: Exception) { }
        }.apply { (layoutParams as LinearLayout.LayoutParams).apply { leftMargin = 0; topMargin = dp(8) } })
        col.addView(nc)

        // ---- gestures card ----
        val gc = card()
        gc.addView(text("Gestures", 17f, ink, true))
        gc.addView(text("\u2022 Tap: open the menu and tap your way through\n\u2022 Hold: open the menu and swipe without lifting\n\u2022 Double tap: open this app\n\u2022 Double tap and hold: notifications\n\u2022 Drag: move the bubble", 14f, muted, false).apply {
            setPadding(0, dp(6), 0, 0)
            setLineSpacing(dp(4).toFloat(), 1f)
        })
        col.addView(gc)

        // ---- custom categories card ----
        val custom = card()
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(text("Custom categories", 17f, ink, true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        head.addView(button("+ Add") { askName("New category", "") { name ->
            val list = Store.customs(this)
            list.add(CustomCat(name, mutableListOf()))
            Store.saveCustoms(this, list)
            drawCustoms()
            pickApps(list.size - 1)
        } })
        custom.addView(head)
        custom.addView(text("They live inside the ⭐ Custom ring. Tap one to choose its apps.", 13f, muted, false).apply {
            setPadding(0, dp(2), 0, dp(8))
        })
        customBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        custom.addView(customBox)
        col.addView(custom)

        col.addView(text("Apps are sorted into categories automatically. Anything not recognised goes into Others.", 13f, muted, false).apply {
            setPadding(dp(4), dp(8), dp(4), 0)
        })

        Thread {
            AppRepo.load(applicationContext)
            runOnUiThread { drawCustoms() }
        }.start()
        drawCustoms()
    }

    private fun paintThemes() {
        val cur = Store.theme(this)
        themeButtons.forEachIndexed { i, b ->
            val sel = i == cur
            b.background = GradientDrawable().apply { setColor(if (sel) accent else Color.rgb(238, 237, 254)); cornerRadius = 16 * d }
            b.setTextColor(if (sel) Color.WHITE else accent)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val can = Settings.canDrawOverlays(this)
        status.text = if (can) "✅  Ready" else "⚠️  Needs permission to float over apps"
        permBtn.visibility = if (can) View.GONE else View.VISIBLE
        val nOn = BubbleNotifListener.isEnabled(this)
        notifStatus.text = if (nOn) "\u2705  Red dot, message previews and music player are on" else "Needed for the red dot, message previews and the music player"
        notifBtn.visibility = if (nOn) View.GONE else View.VISIBLE
        if (nOn && Build.VERSION.SDK_INT >= 24) {
            try { NotificationListenerService.requestRebind(ComponentName(this, BubbleNotifListener::class.java)) } catch (e: Exception) { }
        }
        binding = true
        mainSwitch.isChecked = can && Store.enabled(this)
        binding = false
        if (can && Store.enabled(this)) {
            try { startForegroundService(Intent(this, BubbleService::class.java)) } catch (e: Exception) { }
        }
    }

    private fun drawCustoms() {
        customBox.removeAllViews()
        val list = Store.customs(this)
        list.forEachIndexed { i, c ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(4), 0, dp(4))
            }
            val label = text("📁  ${c.name}   ·   ${c.pkgs.size} apps", 15f, ink, false).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(0, dp(10), 0, dp(10))
                setOnClickListener { pickApps(i) }
            }
            row.addView(label)
            row.addView(smallButton("Rename") {
                askName("Rename", c.name) { name ->
                    val l = Store.customs(this)
                    if (i < l.size) { l[i].name = name; Store.saveCustoms(this, l) }
                    drawCustoms()
                }
            })
            row.addView(smallButton("✕") {
                val l = Store.customs(this)
                if (l.size <= 1) {
                    toast("Keep at least one category")
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Delete ${c.name}?")
                        .setPositiveButton("Delete") { _, _ ->
                            val l2 = Store.customs(this)
                            if (i < l2.size) { l2.removeAt(i); Store.saveCustoms(this, l2) }
                            drawCustoms()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            })
            customBox.addView(row)
        }
    }

    private fun pickApps(index: Int) {
        val apps = AppRepo.apps
        if (apps.isEmpty()) { toast("Still loading your apps, try again in a second"); return }
        val list = Store.customs(this)
        if (index !in list.indices) return
        val c = list[index]
        val labels = apps.map { it.label }.toTypedArray()
        val checked = BooleanArray(apps.size) { c.pkgs.contains(apps[it].pkg) }
        AlertDialog.Builder(this)
            .setTitle("Apps in ${c.name}")
            .setMultiChoiceItems(labels, checked) { _, which, on -> checked[which] = on }
            .setPositiveButton("Save") { _, _ ->
                c.pkgs.clear()
                apps.forEachIndexed { k, a -> if (checked[k]) c.pkgs.add(a.pkg) }
                Store.saveCustoms(this, list)
                drawCustoms()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun askName(title: String, current: String, onOk: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(current)
            hint = "Work, Home, Study…"
            filters = arrayOf(InputFilter.LengthFilter(10))
            setSingleLine()
        }
        val wrap = LinearLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(input, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(wrap)
            .setPositiveButton("OK") { _, _ ->
                val v = input.text.toString().trim()
                if (v.isEmpty()) toast("Enter a name first") else onOk(v)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------- small UI helpers ----------

    private fun text(s: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = 18 * d
            setStroke(dp(1), Color.rgb(226, 224, 240))
        }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(14)
        }
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = GradientDrawable().apply { setColor(accent); cornerRadius = 22 * d }
        setPadding(dp(16), 0, dp(16), 0)
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)).apply { topMargin = dp(8) }
    }

    private fun smallButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 13f
        setTextColor(accent)
        background = GradientDrawable().apply { setColor(Color.rgb(238, 237, 254)); cornerRadius = 16 * d }
        setPadding(dp(12), 0, dp(12), 0)
        minWidth = 0
        minimumWidth = 0
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(34)).apply { leftMargin = dp(6) }
    }

    private fun switch(label: String, initial: Boolean, onChange: (Boolean) -> Unit) = Switch(this).apply {
        text = label
        textSize = 15f
        setTextColor(ink)
        isChecked = initial
        setPadding(0, dp(10), 0, dp(10))
        setOnCheckedChangeListener { _, on -> onChange(on) }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
