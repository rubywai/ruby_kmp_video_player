#import "RubyAVPlayerBridge.h"
#import <objc/runtime.h>
#include <math.h>

static const void *RubyLoopObserverKey = &RubyLoopObserverKey;

static UIWindowScene *ruby_active_window_scene(void) {
    for (UIScene *scene in UIApplication.sharedApplication.connectedScenes) {
        if ([scene isKindOfClass:[UIWindowScene class]] &&
            scene.activationState == UISceneActivationStateForegroundActive) {
            return (UIWindowScene *)scene;
        }
    }
    return nil;
}

void ruby_av_player_configure_audio_session(void) {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryPlayback
                    mode:AVAudioSessionModeMoviePlayback
                 options:AVAudioSessionCategoryOptionAllowBluetoothA2DP
                   error:nil];
    [session setActive:YES error:nil];
}

void ruby_request_landscape_orientation(void) {
    UIWindowScene *scene = ruby_active_window_scene();
    if (@available(iOS 16.0, *)) {
        if (scene != nil) {
            UIWindowSceneGeometryPreferencesIOS *preferences =
                [[UIWindowSceneGeometryPreferencesIOS alloc]
                    initWithInterfaceOrientations:UIInterfaceOrientationMaskLandscape];
            [scene requestGeometryUpdateWithPreferences:preferences errorHandler:nil];
            return;
        }
    }
    UIDevice *device = [UIDevice currentDevice];
    [device setValue:@(UIDeviceOrientationLandscapeRight) forKey:@"orientation"];
    [UIViewController attemptRotationToDeviceOrientation];
}

void ruby_request_portrait_orientation(void) {
    UIWindowScene *scene = ruby_active_window_scene();
    if (@available(iOS 16.0, *)) {
        if (scene != nil) {
            UIWindowSceneGeometryPreferencesIOS *preferences =
                [[UIWindowSceneGeometryPreferencesIOS alloc]
                    initWithInterfaceOrientations:UIInterfaceOrientationMaskPortrait];
            [scene requestGeometryUpdateWithPreferences:preferences errorHandler:nil];
            return;
        }
    }
    UIDevice *device = [UIDevice currentDevice];
    [device setValue:@(UIDeviceOrientationPortrait) forKey:@"orientation"];
    [UIViewController attemptRotationToDeviceOrientation];
}

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

static double ruby_av_player_seconds(CMTime time) {
    if (!CMTIME_IS_NUMERIC(time)) {
        return 0.0;
    }
    double seconds = CMTimeGetSeconds(time);
    return isfinite(seconds) && seconds > 0.0 ? seconds : 0.0;
}

double ruby_av_player_duration_seconds(AVPlayer *player) {
    return ruby_av_player_seconds(player.currentItem.duration);
}

double ruby_av_player_position_seconds(AVPlayer *player) {
    return ruby_av_player_seconds(player.currentTime);
}

double ruby_av_player_buffered_position_seconds(AVPlayer *player) {
    AVPlayerItem *item = player.currentItem;
    if (item == nil || item.loadedTimeRanges.count == 0) {
        return 0.0;
    }
    CMTimeRange range = [item.loadedTimeRanges.lastObject CMTimeRangeValue];
    return ruby_av_player_seconds(CMTimeRangeGetEnd(range));
}

int ruby_av_player_item_status(AVPlayer *player) {
    return (int)player.currentItem.status;
}

static AVAssetVariant *ruby_av_player_hls_variant(AVPlayer *player, int index) {
    if (@available(iOS 15.0, *)) {
        AVURLAsset *asset = (AVURLAsset *)player.currentItem.asset;
        NSArray<AVAssetVariant *> *variants = asset.variants;
        if (index >= 0 && index < (int)variants.count) {
            return variants[(NSUInteger)index];
        }
    }
    return nil;
}

int ruby_av_player_hls_variant_count(AVPlayer *player) {
    if (@available(iOS 15.0, *)) {
        AVURLAsset *asset = (AVURLAsset *)player.currentItem.asset;
        return (int)asset.variants.count;
    }
    return 0;
}

int ruby_av_player_hls_variant_width(AVPlayer *player, int index) {
    AVAssetVariant *variant = ruby_av_player_hls_variant(player, index);
    return variant == nil ? 0 : (int)variant.videoAttributes.presentationSize.width;
}

int ruby_av_player_hls_variant_height(AVPlayer *player, int index) {
    AVAssetVariant *variant = ruby_av_player_hls_variant(player, index);
    return variant == nil ? 0 : (int)variant.videoAttributes.presentationSize.height;
}

double ruby_av_player_hls_variant_peak_bitrate(AVPlayer *player, int index) {
    AVAssetVariant *variant = ruby_av_player_hls_variant(player, index);
    return variant == nil ? 0.0 : variant.peakBitRate;
}

void ruby_av_player_select_hls_variant(AVPlayer *player, int width, int height, double peakBitrate) {
    AVPlayerItem *item = player.currentItem;
    item.preferredMaximumResolution = CGSizeMake(width, height);
    item.preferredPeakBitRate = peakBitrate;
}

void ruby_av_player_select_hls_auto(AVPlayer *player) {
    AVPlayerItem *item = player.currentItem;
    item.preferredMaximumResolution = CGSizeZero;
    item.preferredPeakBitRate = 0.0;
}
