#import "RubyAVPlayerBridge.h"
#import <objc/runtime.h>

static const void *RubyLoopObserverKey = &RubyLoopObserverKey;

void ruby_av_player_load(AVPlayer *player, NSURL *url) {
    AVPlayerItem *item = [AVPlayerItem playerItemWithURL:url];
    [player replaceCurrentItemWithPlayerItem:item];
}

void ruby_av_player_play(AVPlayer *player) {
    [player play];
}

void ruby_av_player_pause(AVPlayer *player) {
    [player pause];
}

void ruby_av_player_stop(AVPlayer *player) {
    [player pause];
    [player replaceCurrentItemWithPlayerItem:nil];
}

void ruby_av_player_seek(AVPlayer *player, double positionSeconds) {
    CMTime time = CMTimeMakeWithSeconds(positionSeconds, 600);
    [player seekToTime:time toleranceBefore:kCMTimeZero toleranceAfter:kCMTimeZero];
}

void ruby_av_player_set_volume(AVPlayer *player, float volume) {
    player.volume = volume;
}

void ruby_av_player_set_rate(AVPlayer *player, float rate) {
    [player setRate:rate];
}

void ruby_av_player_set_looping(AVPlayer *player, bool looping) {
    NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
    id previousObserver = objc_getAssociatedObject(player, RubyLoopObserverKey);
    if (previousObserver != nil) {
        [center removeObserver:previousObserver];
        objc_setAssociatedObject(player, RubyLoopObserverKey, nil, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }

    if (!looping || player.currentItem == nil) {
        return;
    }

    __weak AVPlayer *weakPlayer = player;
    id observer = [center addObserverForName:AVPlayerItemDidPlayToEndTimeNotification
                                      object:player.currentItem
                                       queue:[NSOperationQueue mainQueue]
                                  usingBlock:^(NSNotification *notification) {
        AVPlayer *strongPlayer = weakPlayer;
        if (strongPlayer == nil || notification.object != strongPlayer.currentItem) {
            return;
        }
        [strongPlayer seekToTime:kCMTimeZero toleranceBefore:kCMTimeZero toleranceAfter:kCMTimeZero];
        [strongPlayer play];
    }];
    objc_setAssociatedObject(player, RubyLoopObserverKey, observer, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}
