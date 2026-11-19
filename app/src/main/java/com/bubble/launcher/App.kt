package com.bubble.launcher

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
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
import android.text.InputFilter
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import org.json.JSONArray
import org.json.JSONObject

// ===== AppRepo =====
class AppEntry(val label: String, val pkg: String, val icon: Drawable)

class CategoryDef(val name: String, val emoji: String)

/** Loads every launchable app on the phone and sorts it into a category. */
object AppRepo {

    val CATEGORIES = listOf(
        CategoryDef("AI tools", "✨"),
        CategoryDef("Social", "💬"),
        CategoryDef("Games", "🎮"),
        CategoryDef("Shopping", "🛍️"),
        CategoryDef("Food", "🍔"),
        CategoryDef("10-min", "⚡"),
        CategoryDef("Payments", "💳"),
        CategoryDef("Banks", "🏦"),
        CategoryDef("Calls", "📞"),
        CategoryDef("Media", "🎬"),
        CategoryDef("Travel", "🗺️"),
        CategoryDef("Tools", "🛠️"),
        CategoryDef("Others", "📦")
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

// ===== SoundFx =====
/** Small synthesized UI sounds (no audio files needed). Silent when the phone is on silent/vibrate. */
class SoundFx(ctx: Context) {
    private val rate = 44100
    private val tracks = HashMap<String, AudioTrack>()
    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    var enabled = true

    private class Comp(val noise: Boolean, val f1: Double, val f2: Double, val dur: Double, val gain: Double, val delay: Double = 0.0)

    init {
        try {
            put("open", render(Comp(true, 500.0, 2600.0, 0.30, 0.30), Comp(false, 440.0, 880.0, 0.24, 0.16)))
            put("sub", render(Comp(true, 900.0, 2800.0, 0.20, 0.22), Comp(false, 620.0, 990.0, 0.16, 0.12)))
            put("close", render(Comp(true, 2400.0, 450.0, 0.24, 0.24), Comp(false, 720.0, 330.0, 0.20, 0.12)))
            put("tick", render(Comp(false, 2500.0, 2100.0, 0.03, 0.14)))
            put("page", render(Comp(false, 1200.0, 1600.0, 0.06, 0.16), Comp(false, 1600.0, 2200.0, 0.06, 0.14, 0.07)))
            put("launch", render(Comp(false, 660.0, 660.0, 0.16, 0.18), Comp(false, 990.0, 990.0, 0.30, 0.16, 0.09), Comp(true, 800.0, 3000.0, 0.25, 0.14)))
            put("drop", render(Comp(false, 320.0, 190.0, 0.09, 0.22)))
        } catch (e: Exception) {
            // Sound is optional; ignore failures.
        }
    }

    private fun render(vararg cs: Comp): ShortArray {
        var total = 0.0
        for (c in cs) total = maxOf(total, c.delay + c.dur)
        val n = ((total + 0.02) * rate).toInt()
        val out = DoubleArray(n)
        val rnd = java.util.Random(7)
        for (c in cs) {
            val start = (c.delay * rate).toInt()
            val len = (c.dur * rate).toInt()
            var phase = 0.0
            var lp = 0.0
            var lp2 = 0.0
            for (i in 0 until len) {
                val t = i.toDouble() / rate
                val f = c.f1 * (c.f2 / c.f1).pow(t / c.dur)
                val v: Double
                val env: Double
                if (c.noise) {
                    val a = min(1.0, 2 * PI * f / rate)
                    val x = rnd.nextDouble() * 2 - 1
                    lp += a * (x - lp)
                    lp2 += a * 0.5 * (lp - lp2)
                    v = (lp - lp2) * 3.0
                    env = sin(PI * t / c.dur).pow(1.5)
                } else {
                    phase += 2 * PI * f / rate
                    v = sin(phase)
                    env = min(1.0, t / 0.008) * exp(-4.0 * t / c.dur)
                }
                val idx = start + i
                if (idx < n) out[idx] += v * env * c.gain
            }
        }
        return ShortArray(n) { (out[it].coerceIn(-1.0, 1.0) * 32767).toInt().toShort() }
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

// ===== BubbleView =====
/** The floating glass ball, like AssistiveTouch. */
class BubbleView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val aa = Paint.ANTI_ALIAS_FLAG
    private val base = Paint(aa).apply { color = Color.argb(110, 20, 16, 40) }
    private val glass = Paint(aa)
    private val edge = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f * d; color = Color.argb(190, 255, 255, 255) }
    private val ring = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 2.6f * d; color = Color.argb(235, 255, 255, 255) }
    private val glow = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 7f * d; color = Color.argb(45, 255, 255, 255) }
    private val hl = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f * d; strokeCap = Paint.Cap.ROUND; color = Color.argb(170, 255, 255, 255) }
    private val rect = RectF()
    private var pressedLook = false

    fun setPressedLook(v: Boolean) {
        if (pressedLook != v) { pressedLook = v; invalidate() }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(cx, cy) - 2 * d
        val s = if (pressedLook) 0.86f else 1f
        c.save()
        c.scale(s, s, cx, cy)
        c.drawCircle(cx, cy, r, base)
        glass.shader = LinearGradient(cx - r, cy - r, cx + r, cy + r,
            Color.argb(120, 255, 255, 255), Color.argb(30, 255, 255, 255), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r, glass)
        c.drawCircle(cx, cy, r, edge)
        rect.set(cx - r + 3 * d, cy - r + 3 * d, cx + r - 3 * d, cy + r - 3 * d)
        c.drawArc(rect, 200f, 70f, false, hl)
        val ir = r * 0.45f
        c.drawCircle(cx, cy, ir, glow)
        c.drawCircle(cx, cy, ir, ring)
        c.restore()
    }
}

// ===== RadialMenuView =====
/**
 * Full-screen overlay that draws the glass 300-degree rings and follows the thumb.
 * The touch stream comes from the bubble window (the thumb never lifts), and is fed in via thumbRaw().
 */
@SuppressLint("ViewConstructor")
class RadialMenuView(
    ctx: Context,
    private val sfx: SoundFx,
    private val hap: Haptics,
    private val rootItems: () -> List<Item>,
    private val customItems: () -> List<Item>,
    private val listener: Listener
) : View(ctx) {

    interface Listener {
        fun onLaunch(app: AppEntry)
        fun onClosed()
    }

    enum class Kind { FOLDER, CAT, APP, MORE, EMPTY }

    class Item(
        val kind: Kind,
        val label: String,
        val emoji: String = "",
        val app: AppEntry? = null,
        val apps: (() -> List<AppEntry>)? = null
    )

    private class Frame(val items: List<Item>, val title: String, val radius: Float, val labels: Boolean, val root: Boolean) {
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
        val labels: Boolean, val root: Boolean, val np: Int, val page: Int,
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

    // ---------- paints ----------
    private val aa = Paint.ANTI_ALIAS_FLAG
    private val tint = Paint(aa).apply { color = Color.argb(70, 25, 20, 45) }
    private val glass = Paint(aa)
    private val spec = Paint(aa)
    private val solid = Paint(aa)
    private val edge = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * d }
    private val rimWide = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 7f * d }
    private val rimThin = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f * d; strokeCap = Paint.Cap.ROUND }
    private val bandStroke = Paint(aa).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val lineP = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 2.4f * d; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val emojiP = Paint(aa).apply { textAlign = Paint.Align.CENTER }
    private val labelP = Paint(aa).apply {
        textAlign = Paint.Align.CENTER
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setShadowLayer(4f * d, 0f, 1f * d, Color.argb(170, 0, 0, 0))
    }
    private val arcPath = Path()
    private val bandPath = Path()
    private val rect = RectF()
    private val transparentWhite = Color.argb(0, 255, 255, 255)

    // ---------- public API (called by the service) ----------

    fun openRaw(rawX: Float, rawY: Float, homeRawX: Float, homeRawY: Float) {
        tx = rawX; ty = rawY; homeX = homeRawX; homeY = homeRawY
        pending = true
        if (width > 0) startPending()
    }

    fun thumbRaw(rawX: Float, rawY: Float) {
        if (closing) return
        if (!started) { tx = rawX; ty = rawY; return }
        getLocationOnScreen(loc)
        thumb(rawX - loc[0], rawY - loc[1])
    }

    fun release() {
        if (closing) return
        closing = true
        cancelDwell()
        val r = cur
        val item = if (r != null && hi >= 0 && hi < r.vis.size) r.vis[hi] else null
        val app = item?.app
        if (r != null && item != null && item.kind == Kind.APP && app != null) {
            sfx.play("launch"); hap.heavy()
            dismissCurrent(r.cx, r.cy, hi)
            listener.onLaunch(app)
        } else {
            sfx.play("close"); hap.tick()
            dismissCurrent(homeX, homeY, -1)
        }
        postDelayed({ listener.onClosed() }, 460)
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
        pushRing(rootItems(), tx, ty, null, null, r1, true, "")
    }

    // ---------- ring logic ----------

    private fun now() = SystemClock.uptimeMillis()
    private fun deg(r: Float) = r * 57.29578f
    private fun rad(dg: Float) = dg * 0.017453292f
    private fun clampF(v: Float, lo: Float, hi: Float) = if (hi < lo) (lo + hi) / 2f else v.coerceIn(lo, hi)
    private fun argbW(a: Float) = Color.argb(a.toInt().coerceIn(0, 255), 255, 255, 255)
    private fun ang(r: Ring, i: Int) = rad(r.base + r.step * (i + 0.5f))

    private fun pushRing(items: List<Item>, px: Float, py: Float, fromX: Float?, fromY: Float?, radius: Float, labels: Boolean, title: String) {
        val f = Frame(items, title, radius, labels, stack.isEmpty())
        val m = radius + 46 * d
        f.cx = clampF(px, m, width - m)
        f.cy = clampF(py, m, height - m)
        val dist = hypot(px - f.cx, py - f.cy)
        if (dist > 30 * d) {
            // Ring was pushed away from a screen edge: put the gap (and back button) right under the thumb.
            f.gap = deg(atan2(py - f.cy, px - f.cx))
            f.backX = px; f.backY = py
        } else {
            f.gap = if (fromX != null && fromY != null) deg(atan2(fromY - f.cy, fromX - f.cx)) else 90f
            f.backX = f.cx; f.backY = f.cy
        }
        stack.add(f)
        render(f, px, py)
    }

    private fun render(f: Frame, ox: Float, oy: Float) {
        hi = -1; left = false; armed = false; armX = tx; armY = ty
        cancelDwell()
        val n = f.items.size
        if (n > maxItems) {
            val per = maxItems - 1
            f.np = (n + per - 1) / per
            if (f.page >= f.np) f.page = 0
            val s = f.page * per
            f.vis = f.items.subList(s, min(n, s + per)) + Item(Kind.MORE, "More")
        } else {
            f.np = 1; f.page = 0; f.vis = f.items
        }
        val step = 300f / f.vis.size
        val r = Ring(f, f.vis, f.cx, f.cy, f.radius, f.gap + 30f, step, f.backX, f.backY,
            f.labels, f.root, f.np, f.page, ox, oy, now())
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

    private fun thumb(x: Float, y: Float) {
        tx = x; ty = y
        val r = cur ?: return
        if (!armed && hypot(x - armX, y - armY) > 30 * d) armed = true
        val dB = hypot(x - r.backX, y - r.backY)
        if (dB > 45 * d) left = true
        if (!r.root && left && dB < 24 * d) { pop(); return }
        val p = pick(r, x, y)
        if (p != hi) {
            hi = p
            cancelDwell()
            if (p >= 0) { sfx.play("tick"); hap.tick() }
            invalidate()
        }
        if (hi < 0) return
        val item = r.vis[hi]
        when (item.kind) {
            Kind.MORE -> if (dwell == null) {
                val run = Runnable { dwell = null; flip(r.f) }
                dwell = run
                postDelayed(run, 450)
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

    private fun flip(f: Frame) {
        if (closing || cur?.f !== f) return
        sfx.play("page"); hap.click()
        dismissCurrent(f.cx, f.cy, -1)
        f.page = (f.page + 1) % f.np
        render(f, f.cx, f.cy)
        armed = true
    }

    private fun openChild(r: Ring, item: Item, x: Float, y: Float) {
        sfx.play("sub"); hap.click()
        dismissCurrent(x, y, -1)
        val folder = item.kind == Kind.FOLDER
        val items = if (folder) customItems() else appItems(item.apps?.invoke() ?: emptyList())
        pushRing(items, x, y, r.cx, r.cy, if (folder) r1 else r2, folder, item.label)
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
        if (list.isEmpty()) listOf(Item(Kind.EMPTY, "Empty", "＋"))
        else list.map { Item(Kind.APP, it.label, app = it) }

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
        drawTray(c, r, trayS, trayA)

        for (i in r.vis.indices) {
            val target = if (r === cur && i == hi) 1f else 0f
            val h = r.hl[i]
            val nh = h + (target - h) * 0.35f
            if (kotlin.math.abs(target - nh) < 0.01f) r.hl[i] = target else { r.hl[i] = nh; anim = true }
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

    private fun drawTray(c: Canvas, r: Ring, s: Float, a: Float) {
        if (a <= 0.01f) return
        val rIn = r.radius - 31 * d
        val rOut = r.radius + 38 * d
        val mid = (rIn + rOut) / 2f
        val w = rOut - rIn
        val cap = deg(w / 2f / mid)
        val sweep = 300f - 2f * cap
        c.save()
        c.scale(s, s, r.cx, r.cy)

        rect.set(r.cx - mid, r.cy - mid, r.cx + mid, r.cy + mid)
        arcPath.reset()
        arcPath.addArc(rect, r.base + cap, sweep)
        bandStroke.strokeWidth = w
        bandPath.reset()
        @Suppress("DEPRECATION")
        bandStroke.getFillPath(arcPath, bandPath)

        // dark tint so the glass reads on bright screens too
        tint.color = Color.argb((80 * a).toInt(), 25, 20, 45)
        c.drawPath(bandPath, tint)
        // frosted white body
        glass.shader = LinearGradient(
            r.cx - rOut, r.cy - rOut, r.cx + rOut, r.cy + rOut,
            intArrayOf(argbW(105 * a), argbW(25 * a), argbW(60 * a)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        c.drawPath(bandPath, glass)
        glass.shader = null
        // specular sheen at top-left
        spec.shader = SweepGradient(
            r.cx, r.cy,
            intArrayOf(transparentWhite, transparentWhite, argbW(120 * a), transparentWhite, transparentWhite),
            floatArrayOf(0f, 0.56f, 0.64f, 0.72f, 1f)
        )
        c.drawPath(bandPath, spec)
        spec.shader = null
        // bright outer edge
        edge.color = argbW(175 * a)
        c.drawPath(bandPath, edge)
        // inner rim glow
        val ri = rIn + 3 * d
        rect.set(r.cx - ri, r.cy - ri, r.cx + ri, r.cy + ri)
        rimWide.color = argbW(35 * a)
        c.drawArc(rect, r.base + cap, sweep, false, rimWide)
        rimThin.color = argbW(175 * a)
        c.drawArc(rect, r.base + cap, sweep, false, rimThin)
        c.restore()
    }

    private fun glassCircle(c: Canvas, x: Float, y: Float, rad: Float, a: Float, h: Float) {
        tint.color = Color.argb((70 * a).toInt().coerceIn(0, 255), 25, 20, 45)
        c.drawCircle(x, y, rad, tint)
        glass.shader = LinearGradient(x - rad, y - rad, x + rad, y + rad, argbW(115 * a), argbW(25 * a), Shader.TileMode.CLAMP)
        c.drawCircle(x, y, rad, glass)
        glass.shader = null
        if (h > 0f) {
            solid.color = argbW(235 * h * a)
            c.drawCircle(x, y, rad, solid)
        }
        edge.color = argbW(160 * a)
        c.drawCircle(x, y, rad, edge)
        val k = 3 * d
        rect.set(x - rad + k, y - rad + k, x + rad - k, y + rad - k)
        rimThin.color = argbW(130 * a)
        c.drawArc(rect, 200f, 70f, false, rimThin)
    }

    private fun drawItem(c: Canvas, r: Ring, item: Item, x: Float, y: Float, sc: Float, a: Float, h: Float) {
        if (a <= 0.01f) return
        val rad = 22 * d * sc
        val app = item.app
        if (item.kind == Kind.APP && app != null) {
            solid.color = argbW((45 + 160 * h) * a)
            c.drawCircle(x, y, rad + 3 * d * sc, solid)
            val ic = app.icon
            ic.setBounds((x - rad).toInt(), (y - rad).toInt(), (x + rad).toInt(), (y + rad).toInt())
            ic.alpha = (255 * a).toInt().coerceIn(0, 255)
            ic.draw(c)
        } else {
            glassCircle(c, x, y, rad, a, h)
            val txt = if (item.kind == Kind.MORE) "•••" else item.emoji
            emojiP.textSize = (if (item.kind == Kind.MORE) 14f else 20f) * d * sc
            emojiP.color = if (h > 0.5f) Color.rgb(38, 33, 92) else Color.WHITE
            emojiP.alpha = (255 * a).toInt().coerceIn(0, 255)
            val fm = emojiP.fontMetrics
            c.drawText(txt, x, y - (fm.ascent + fm.descent) / 2f, emojiP)
        }
        if (r.labels || h > 0.05f) {
            var label = when {
                item.kind == Kind.MORE -> if (h > 0.05f) "Hold for next page" else ""
                else -> item.label
            }
            if (label.isEmpty()) return
            if (label.length > 14) label = label.take(13) + "…"
            labelP.textSize = 12f * d * (if (r.labels) 1f else 1.1f)
            labelP.alpha = (255 * a * (if (r.labels) 1f else h)).toInt().coerceIn(0, 255)
            c.drawText(label, x, y + rad + 15 * d, labelP)
        }
    }

    private fun drawBack(c: Canvas, r: Ring, s: Float, a: Float) {
        if (a <= 0.01f) return
        val x = r.backX
        val y = r.backY
        val rad = 22 * d * s
        glassCircle(c, x, y, rad, a, 0f)
        lineP.color = argbW(240 * a)
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
}

// ===== BubbleService =====
/** Keeps the bubble floating over every app and runs the hold-and-swipe gesture. */
class BubbleService : Service() {

    private var wm: WindowManager? = null
    private var bubble: BubbleView? = null
    private var bp: WindowManager.LayoutParams? = null
    private var menu: RadialMenuView? = null
    private var sfx: SoundFx? = null
    private var hap: Haptics? = null
    private val h = Handler(Looper.getMainLooper())
    private val dens by lazy { resources.displayMetrics.density }

    private var mode = 0 // 0 idle, 1 pressed, 2 dragging, 3 menu open
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0
    private var startY = 0
    private var receiverOn = false

    private val holdRunnable = Runnable { if (mode == 1) { mode = 3; openMenu() } }
    private val idleRunnable = Runnable { if (mode == 0) bubble?.animate()?.alpha(0.5f)?.setDuration(300)?.start() }

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
        sfx = SoundFx(this)
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
        val size = (60 * dens).toInt()
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
        v.setOnTouchListener { _, e -> onTouch(e); true }
        w.addView(v, p)
        bubble = v
        bp = p
        scheduleIdle()
    }

    private fun wake() {
        h.removeCallbacks(idleRunnable)
        bubble?.animate()?.cancel()
        bubble?.alpha = 1f
    }

    private fun scheduleIdle() {
        h.removeCallbacks(idleRunnable)
        h.postDelayed(idleRunnable, 2500)
    }

    private fun onTouch(e: MotionEvent) {
        val p = bp ?: return
        val w = wm ?: return
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                wake()
                mode = 1
                downX = e.rawX; downY = e.rawY; lastX = e.rawX; lastY = e.rawY
                startX = p.x; startY = p.y
                bubble?.setPressedLook(true)
                h.postDelayed(holdRunnable, 280)
            }
            MotionEvent.ACTION_MOVE -> {
                lastX = e.rawX; lastY = e.rawY
                if (mode == 1 && hypot(e.rawX - downX, e.rawY - downY) > 8 * dens) {
                    h.removeCallbacks(holdRunnable)
                    mode = 2
                    bubble?.setPressedLook(false)
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
                bubble?.setPressedLook(false)
                when (mode) {
                    2 -> snapToEdge()
                    3 -> menu?.release()
                }
                mode = 0
                scheduleIdle()
            }
        }
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

    private fun openMenu() {
        val w = wm ?: return
        val s = sfx ?: return
        val hp = hap ?: return
        menu?.let { old -> try { w.removeViewImmediate(old) } catch (e: Exception) { } }
        menu = null
        s.enabled = Store.sound(this)
        hp.enabled = Store.haptics(this)
        s.play("open")
        hp.click()

        val mv = RadialMenuView(this, s, hp, ::rootItems, ::customItems, object : RadialMenuView.Listener {
            override fun onLaunch(app: AppEntry) = launch(app)
            override fun onClosed() = closeMenu()
        })
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.dimAmount = 0.28f
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= 31 && Store.blur(this)) {
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            lp.blurBehindRadius = (22 * dens).toInt()
        }
        try {
            w.addView(mv, lp)
        } catch (e: Exception) {
            return
        }
        menu = mv
        val loc = IntArray(2)
        bubble?.getLocationOnScreen(loc)
        val half = (bp?.width ?: 0) / 2f
        mv.openRaw(lastX, lastY, loc[0] + half, loc[1] + half)
        bubble?.animate()?.alpha(0.2f)?.setDuration(150)?.start()
    }

    private fun closeMenu() {
        val m = menu ?: return
        try { wm?.removeViewImmediate(m) } catch (e: Exception) { }
        if (menu === m) menu = null
        bubble?.animate()?.alpha(1f)?.setDuration(200)?.start()
        scheduleIdle()
    }

    private fun launch(app: AppEntry) {
        val i = packageManager.getLaunchIntentForPackage(app.pkg) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try { startActivity(i) } catch (e: Exception) { }
    }

    private fun rootItems(): List<RadialMenuView.Item> {
        val out = ArrayList<RadialMenuView.Item>()
        out.add(RadialMenuView.Item(RadialMenuView.Kind.FOLDER, "Custom", "⭐"))
        for (cd in AppRepo.CATEGORIES) {
            val apps = AppRepo.appsIn(cd.name)
            if (apps.isNotEmpty()) out.add(RadialMenuView.Item(RadialMenuView.Kind.CAT, cd.name, cd.emoji, apps = { apps }))
        }
        return out
    }

    private fun customItems(): List<RadialMenuView.Item> {
        val list = Store.customs(this).map { cc ->
            val apps = cc.pkgs.mapNotNull { AppRepo.byPkg(it) }
            RadialMenuView.Item(RadialMenuView.Kind.CAT, cc.name, "📁", apps = { apps })
        }
        return list.ifEmpty { listOf(RadialMenuView.Item(RadialMenuView.Kind.EMPTY, "No categories", "＋")) }
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
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
    private var binding = false

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

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val can = Settings.canDrawOverlays(this)
        status.text = if (can) "✅  Ready" else "⚠️  Needs permission to float over apps"
        permBtn.visibility = if (can) View.GONE else View.VISIBLE
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
