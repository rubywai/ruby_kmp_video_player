#import <AVFoundation/AVFoundation.h>
#include <stdbool.h>

void ruby_av_player_load(AVPlayer *player, NSURL *url);
void ruby_av_player_play(AVPlayer *player);
void ruby_av_player_pause(AVPlayer *player);
void ruby_av_player_stop(AVPlayer *player);
void ruby_av_player_seek(AVPlayer *player, double positionSeconds);
void ruby_av_player_set_volume(AVPlayer *player, float volume);
void ruby_av_player_set_rate(AVPlayer *player, float rate);
void ruby_av_player_set_looping(AVPlayer *player, bool looping);
