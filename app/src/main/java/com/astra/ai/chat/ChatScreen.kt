
package com.astra.ai.chat

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.astra.ai.HudView
import com.astra.ai.OrbView
import com.astra.ai.OrbState
import com.astra.ai.Tema

class ChatScreen(
    context: Context,
    private val store: ChatStore,
    private val onSend: (String) -> Unit,
    private val onVoice: () -> Unit
) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val messages = LinearLayout(context)
    private val scroll = ScrollView(context)
    private val input = EditText(context)
    private val status = TextView(context)

    private val orb = OrbView(context)
    private val hud = HudView(context)

    var conversationId: Long = store.getLatestConversation()
        private set

    private fun dp(value: Float) =
        (value * density).toInt()

    init {
        orientation = VERTICAL
        setPadding(dp(16f), dp(10f), dp(16f), dp(12f))

        buildHeader()
        buildConversation()
        buildComposer()

        loadConversation(conversationId)
    }

    private fun buildHeader() {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val history = actionButton("☰") {
            showHistory()
        }

        val title = TextView(context).apply {
            text = "ASTRA AI"
            textSize = 17f
            letterSpacing = 0.28f
            gravity = Gravity.CENTER
            setTextColor(Tema.texto)
            layoutParams = LayoutParams(0, dp(48f), 1f)
        }

        val newChat = actionButton("＋") {
            newConversation()
        }

        header.addView(history)
        header.addView(title)
        header.addView(newChat)

        addView(
            header,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48f)
            )
        )

        status.text = "Tu asistente personal"
        status.textSize = 12f
        status.gravity = Gravity.CENTER
        status.setTextColor(Tema.cian)
        status.setPadding(0, dp(4f), 0, dp(10f))

        addView(
            status,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(30f)
            )
        )
    }

    private fun buildConversation() {
        messages.orientation = VERTICAL
        messages.setPadding(
            dp(2f), dp(12f), dp(2f), dp(110f)
        )

        scroll.isFillViewport = true
        scroll.isVerticalScrollBarEnabled = false

        scroll.addView(
            messages,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val layer = FrameLayout(context)

        layer.addView(
            scroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        // Esfera flotante de Astra.
        val floatingOrb = FrameLayout(context).apply {
            background = panel(
                Color.argb(105, 13, 23, 53),
                Tema.cian,
                100,
                50f
            )

            elevation = dp(10f).toFloat()
            isClickable = true
            isFocusable = true
            contentDescription = "Hablar con Astra"

            setOnClickListener {
                onVoice()
            }

            addView(
                hud,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            addView(
                orb,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }

        layer.addView(
            floatingOrb,
            FrameLayout.LayoutParams(
                dp(82f),
                dp(82f),
                Gravity.BOTTOM or Gravity.END
            ).apply {
                rightMargin = dp(8f)
                bottomMargin = dp(12f)
            }
        )

        addView(
            layer,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
    }

    private fun buildComposer() {
        val composer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            background = panel(
                Color.argb(190, 18, 28, 58),
                Tema.cian,
                125,
                30f
            )

            setPadding(dp(8f), dp(5f), dp(7f), dp(5f))
        }

        val voice = actionButton("🎙") {
            onVoice()
        }

        input.hint = "Pregúntale algo a Astra..."
        input.setHintTextColor(Tema.textoSuave)
        input.setTextColor(Tema.texto)
        input.textSize = 15f
        input.maxLines = 4
        input.minLines = 1

        input.inputType =
            InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE

        input.imeOptions = EditorInfo.IME_ACTION_SEND

        input.setPadding(
            dp(10f), dp(8f), dp(8f), dp(8f)
        )

        input.background = null

        input.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEND) {
                sendInput()
                true
            } else {
                false
            }
        }

        val send = actionButton("➤") {
            sendInput()
        }

        composer.addView(voice)

        composer.addView(
            input,
            LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        composer.addView(send)

        addView(
            composer,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun actionButton(
        label: String,
        action: () -> Unit
    ) = TextView(context).apply {

        text = label
        textSize = 19f
        gravity = Gravity.CENTER
        setTextColor(Tema.texto)

        background = panel(
            Color.argb(145, 25, 36, 73),
            Tema.violeta,
            95,
            24f
        )

        isClickable = true
        isFocusable = true

        setOnClickListener {
            action()
        }

        layoutParams = LayoutParams(
            dp(46f),
            dp(46f)
        ).apply {
            leftMargin = dp(2f)
            rightMargin = dp(2f)
        }
    }

    private fun panel(
        fill: Int,
        border: Int,
        alpha: Int,
        radius: Float
    ) = GradientDrawable().apply {

        cornerRadius = dp(radius).toFloat()
        setColor(fill)

        setStroke(
            dp(1f).coerceAtLeast(1),
            Color.argb(
                alpha,
                Color.red(border),
                Color.green(border),
                Color.blue(border)
            )
        )
    }

    private fun sendInput() {
        val text = input.text.toString().trim()

        if (text.isEmpty()) return

        input.setText("")
        onSend(text)
    }

    fun addMessage(role: String, content: String) {
        store.addMessage(
            conversationId,
            role,
            content
        )

        renderMessage(role, content)
    }

    fun loadConversation(id: Long) {
        conversationId = id
        messages.removeAllViews()

        val history = store.getMessages(id)

        if (history.isEmpty()) {
            renderWelcome()
        } else {
            history.forEach {
                renderMessage(it.role, it.content)
            }
        }

        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun newConversation() {
        conversationId = store.createConversation()
        loadConversation(conversationId)
    }

    fun setStatus(value: String) {
        status.text = value
    }

    fun setOrbState(state: OrbState) {
        orb.ponerEstado(state)
        hud.ponerEstado(state)
    }

    fun setOrbLevel(level: Float) {
        orb.ponerNivel(level)
    }

    private fun renderWelcome() {
        val welcome = TextView(context).apply {
            text = """
                Hola, soy Astra.

                Puedo ayudarte con tu teléfono,
                ejecutar acciones, buscar música
                y responder tus preguntas.

                ¿Qué hacemos?
            """.trimIndent()

            textSize = 17f
            setTextColor(Tema.texto)
            setLineSpacing(dp(4f).toFloat(), 1f)

            background = panel(
                Color.argb(155, 19, 31, 67),
                Tema.violeta,
                110,
                22f
            )

            setPadding(
                dp(18f), dp(16f), dp(18f), dp(16f)
            )
        }

        messages.addView(
            welcome,
            LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                rightMargin = dp(42f)
                bottomMargin = dp(14f)
            }
        )
    }

    private fun renderMessage(
        role: String,
        content: String
    ) {
        val isUser = role == "user"

        val bubble = TextView(context).apply {
            text = content
            textSize = 16f
            setTextColor(Tema.texto)
            setLineSpacing(dp(3f).toFloat(), 1f)

            background = if (isUser) {
                panel(
                    Color.argb(220, 48, 69, 91),
                    Tema.cian,
                    95,
                    22f
                )
            } else {
                panel(
                    Color.argb(185, 20, 31, 68),
                    Tema.violeta,
                    120,
                    22f
                )
            }

            setPadding(
                dp(16f), dp(12f), dp(16f), dp(12f)
            )
        }

        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
        }

        if (!isUser) {
            val avatar = TextView(context).apply {
                text = "A"
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)

                background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(Tema.violeta, Tema.cian)
                ).apply {
                    shape = GradientDrawable.OVAL
                }
            }

            row.addView(
                avatar,
                LayoutParams(dp(32f), dp(32f)).apply {
                    rightMargin = dp(8f)
                }
            )
        }

        row.addView(
            bubble,
            LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                if (isUser) {
                    leftMargin = dp(44f)
                } else {
                    rightMargin = dp(2f)
                }
            }
        )

        messages.addView(
            row,
            LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12f)
            }
        )

        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showHistory() {
        val conversations = store.getConversations()

        val labels = arrayOf(
            "＋  Nueva conversación"
        ) + conversations.map {
            it.title.ifBlank {
                "Nueva conversación"
            }.take(55)
        }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Tus conversaciones")
            .setItems(labels) { _, which ->
                if (which == 0) {
                    newConversation()
                } else {
                    loadConversation(
                        conversations[which - 1].id
                    )
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }
}
