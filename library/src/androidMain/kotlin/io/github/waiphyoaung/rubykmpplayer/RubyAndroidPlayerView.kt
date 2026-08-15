package io.github.waiphyoaung.rubykmpplayer

import android.content.Context
import android.util.AttributeSet
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@UnstableApi
public class RubyAndroidPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : PlayerView(context, attrs, defStyleAttr) {
    public fun bind(controller: RubyAndroidVideoPlayerController) {
        player = controller.exoPlayer
    }

    public fun unbind() {
        player = null
    }
}
