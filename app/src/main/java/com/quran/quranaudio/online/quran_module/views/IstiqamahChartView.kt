package com.quran.quranaudio.online.quran_module.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * 🕌 Istiqamah 引导页的渐变曲线图表
 * 
 * 显示一条带有起伏波动的成长曲线，象征持续努力中的挑战与坚持
 * 
 * 特性：
 * 1. 时间轴扩展至6个月（Jan-Jun）
 * 2. 曲线有明显的起伏：上升 -> 下降（挑战）-> 回升（坚持）
 * 3. 起始点从30%开始（有基础信仰）
 * 4. 颜色渐变强化"挣扎"区域（橙色/黄色代表Qada'弥补）
 * 5. 曲线加粗，更易识别
 * 6. 曲线下方至底部有浅色渐变填充效果
 */
class IstiqamahChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 曲线画笔（加粗线条）
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f  // 加粗的线条，更易识别
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    // 填充区域画笔（渐变效果）
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    // 时间轴画笔
    private val axisLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.parseColor("#40FFFFFF")  // 半透明白色
    }

    private val curvePath = Path()
    private val fillPath = Path()
    private var lineGradient: LinearGradient? = null
    private var fillGradient: LinearGradient? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // 创建曲线的渐变（从橙色到黄色到绿色，强化"挣扎"区域）
        lineGradient = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            intArrayOf(
                Color.parseColor("#FF9800"), // 起点：橙色（初始努力）
                Color.parseColor("#FFC107"), // 1/3处：黄色（挣扎/Qada'弥补）
                Color.parseColor("#FFD54F"), // 中间：亮黄色（挑战期）
                Color.parseColor("#AED581"), // 2/3处：浅绿（恢复期）
                Color.parseColor("#66BB6A")  // 终点：绿色（稳定成长）
            ),
            floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        linePaint.shader = lineGradient
        
        // 创建填充区域的渐变（顶部透明，底部稍微不透明）
        fillGradient = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                Color.parseColor("#30FFFFFF"), // 顶部：半透明白色
                Color.parseColor("#05FFFFFF")  // 底部：几乎透明
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = fillGradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (width == 0 || height == 0) return
        
        val chartHeight = height * 0.85f  // 留出底部空间给时间轴
        val bottomY = chartHeight
        
        // 定义6个时间节点（Jan-Jun），展现真实的成长曲线（有起伏和波动）
        // Y值：0.0=顶部, 1.0=底部。起始点从0.7（30%高度）开始
        val timePoints = listOf(
            0f to 0.70f,        // Jan: 起点 30% （有基础信仰）
            0.15f to 0.55f,     // Jan末: 上升至 45%（初期热情）
            0.25f to 0.40f,     // Feb: 继续上升至 60%（持续努力）
            0.40f to 0.60f,     // Mar初: 下降至 40%（挑战/怠惰期）
            0.55f to 0.50f,     // Mar末: 小幅回升至 50%（Qada'弥补）
            0.70f to 0.35f,     // Apr末: 恢复上升至 65%（重拾坚持）
            0.85f to 0.25f,     // May末: 稳定上升至 75%（建立习惯）
            1f to 0.15f         // Jun: 达到 85%（持续成长）
        )
        
        // 在每两个时间点之间插入中间点，创建更平滑的曲线
        val smoothPoints = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until timePoints.size - 1) {
            val current = timePoints[i]
            val next = timePoints[i + 1]
            
            smoothPoints.add(current)
            
            // 添加2个中间点
            val x1 = current.first + (next.first - current.first) * 0.33f
            val y1 = current.second + (next.second - current.second) * 0.33f
            smoothPoints.add(x1 to y1)
            
            val x2 = current.first + (next.first - current.first) * 0.66f
            val y2 = current.second + (next.second - current.second) * 0.66f
            smoothPoints.add(x2 to y2)
        }
        smoothPoints.add(timePoints.last())
        
        // 将相对坐标转换为实际像素坐标
        val pixelPoints = smoothPoints.map { (x, y) ->
            x * width to y * chartHeight
        }
        
        // 绘制时间轴（底部横线）
        canvas.drawLine(0f, bottomY, width.toFloat(), bottomY, axisLinePaint)
        
        // 绘制时间节点的竖线
        timePoints.forEach { (x, _) ->
            val xPos = x * width
            canvas.drawLine(xPos, bottomY - 4f, xPos, bottomY + 4f, axisLinePaint)
        }
        
        // === 绘制填充区域（曲线下方的渐变） ===
        fillPath.reset()
        
        // 从左下角开始
        fillPath.moveTo(0f, bottomY)
        
        // 移动到曲线起点
        fillPath.lineTo(pixelPoints[0].first, bottomY)
        fillPath.lineTo(pixelPoints[0].first, pixelPoints[0].second)
        
        // 沿着曲线绘制（使用平滑曲线）
        for (i in 0 until pixelPoints.size - 1) {
            val current = pixelPoints[i]
            val next = pixelPoints[i + 1]
            
            // 计算控制点
            val controlX = (current.first + next.first) / 2
            val controlY = (current.second + next.second) / 2
            
            fillPath.quadTo(controlX, controlY, next.first, next.second)
        }
        
        // 连接到右下角
        fillPath.lineTo(width.toFloat(), bottomY)
        fillPath.close()
        
        canvas.drawPath(fillPath, fillPaint)
        
        // === 绘制曲线本身 ===
        curvePath.reset()
        curvePath.moveTo(pixelPoints[0].first, pixelPoints[0].second)
        
        for (i in 0 until pixelPoints.size - 1) {
            val current = pixelPoints[i]
            val next = pixelPoints[i + 1]
            
            // 计算控制点（创建平滑曲线）
            val controlX = (current.first + next.first) / 2
            val controlY = (current.second + next.second) / 2
            
            curvePath.quadTo(controlX, controlY, next.first, next.second)
        }
        
        canvas.drawPath(curvePath, linePaint)
        
        // === 绘制时间节点的小圆点 ===
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        
        timePoints.forEachIndexed { index, (x, y) ->
            val xPos = x * width
            val yPos = y * chartHeight
            
            // 外圈（带渐变色）
            val outerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = when (index) {
                    0 -> Color.parseColor("#E57373")  // 红色
                    1 -> Color.parseColor("#FFB74D")  // 橙色
                    2 -> Color.parseColor("#AED581")  // 黄绿色
                    else -> Color.parseColor("#81C784") // 绿色
                }
            }
            canvas.drawCircle(xPos, yPos, 6f, outerDotPaint)
            
            // 内圈（白色）
            canvas.drawCircle(xPos, yPos, 3f, dotPaint)
        }
    }
}

