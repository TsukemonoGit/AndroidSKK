package jp.deadend.noname.skk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CandidatesViewTest {

    @Test
    fun testSetContentsAndDraw() {
        val service = Robolectric.buildService(SKKService::class.java).create().get()
        val container = service.onCreateCandidatesView()
        val view = container.findViewById<CandidatesView>(R.id.candidates)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 400, 200)

        view.setContents(listOf("候補1", "候補2"), "かんじ")

        val list = candidateList(view)
        assertEquals(listOf("候補1", "候補2"), list)

        val bitmap = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        assertFalse(drawingFlag(view))
    }

    @Test
    fun testSetContentsDoesNotWaitForDrawingFlag() {
        val service = Robolectric.buildService(SKKService::class.java).create().get()
        val container = service.onCreateCandidatesView()
        val view = container.findViewById<CandidatesView>(R.id.candidates)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, 400, 200)

        // 旧実装は mDrawing が true の間 runBlocking で待ち続けた。
        setDrawingFlag(view, true)
        view.setContents(listOf("候補"), "")
        assertEquals(listOf("候補"), candidateList(view))
        assertTrue("setContents は描画フラグを待たない", true)
    }

    @Test
    fun testMoveWithUnknownPointerIsIgnored() {
        val service = Robolectric.buildService(SKKService::class.java).create().get()
        val container = service.onCreateCandidatesView()
        val view = container.findViewById<View>(R.id.candidates_left)
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 10f, 10f, 0)
        try {
            // DOWN を取りこぼしたポインタでも IndexOutOfBounds にせず処理済みにする
            assertTrue(view.dispatchTouchEvent(event))
        } finally {
            event.recycle()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun candidateList(view: CandidatesView): List<String> {
        val f = CandidatesView::class.java.getDeclaredField("mCandidateList")
        f.isAccessible = true
        return (f.get(view) as MutableList<String>).toList()
    }

    private fun drawingFlag(view: CandidatesView): Boolean {
        val f = CandidatesView::class.java.getDeclaredField("mDrawing")
        f.isAccessible = true
        return f.getBoolean(view)
    }

    private fun setDrawingFlag(view: CandidatesView, value: Boolean) {
        val f = CandidatesView::class.java.getDeclaredField("mDrawing")
        f.isAccessible = true
        f.setBoolean(view, value)
    }
}
