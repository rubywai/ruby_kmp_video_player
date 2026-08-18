#import <AVFoundation/AVFoundation.h>

void ruby_av_player_load(AVPlayer *player, NSURL *url);
void ruby_av_player_play(AVPlayer *player);
void ruby_av_player_pause(AVPlayer *player);
void ruby_av_player_stop(AVPlayer *player);
void ruby_av_player_seek(AVPlayer *player, double positionSeconds);
