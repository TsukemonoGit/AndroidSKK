package jp.deadend.noname.skk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.PopupWindow
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * キーボード描画・ポップアップ表示が例外で IME を落とさないことを確認する。
 * クラッシュログ由来の「key label has 2 lines」と BadTokenException の再発防止。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KeyboardViewDrawTest {

    @Test
    fun testUnexpectedTwoLineLabelDoesNotCrashOnDraw() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = KeyboardView(context, null)
        val keyboard = Keyboard(context, R.xml.qwerty, 1080, 800)
        // 通常は1行か3行だが、想定外の2行ラベルでも描画で落ちないこと。
        keyboard.keys.first { it.label.isNotEmpty() }.label = "a\nb"
        view.keyboard = keyboard
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 1080, 800)

        view.draw(Canvas(Bitmap.createBitmap(1080, 800, Bitmap.Config.ARGB_8888)))
    }

    @Test
    fun testPopupIsNotShownWhenAnchorIsDetached() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = object : KeyboardView(context, null) {
            fun show(popup: PopupWindow): Boolean = showPopupSafely(popup, this, 0, 0)
        }
        val popup = PopupWindow(View(context))

        // ウィンドウトークンが無い状態では showAtLocation を呼ばず false を返す。
        assertFalse(view.show(popup))
    }
}
