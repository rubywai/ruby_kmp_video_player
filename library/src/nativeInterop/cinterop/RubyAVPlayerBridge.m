#import "RubyAVPlayerBridge.h"

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
