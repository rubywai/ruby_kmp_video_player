#import <AVFoundation/AVFoundation.h>
#import <UIKit/UIKit.h>
#include <stdbool.h>

void ruby_av_player_load(AVPlayer *player, NSURL *url);
void ruby_av_player_configure_audio_session(void);
void ruby_request_landscape_orientation(void);
void ruby_request_portrait_orientation(void);
void ruby_av_player_play(AVPlayer *player);
void ruby_av_player_pause(AVPlayer *player);
void ruby_av_player_stop(AVPlayer *player);
void ruby_av_player_seek(AVPlayer *player, double positionSeconds);
void ruby_av_player_set_volume(AVPlayer *player, float volume);
void ruby_av_player_set_rate(AVPlayer *player, float rate);
void ruby_av_player_set_looping(AVPlayer *player, bool looping);
double ruby_av_player_duration_seconds(AVPlayer *player);
double ruby_av_player_position_seconds(AVPlayer *player);
double ruby_av_player_buffered_position_seconds(AVPlayer *player);
int ruby_av_player_item_status(AVPlayer *player);
int ruby_av_player_hls_variant_count(AVPlayer *player);
int ruby_av_player_hls_variant_width(AVPlayer *player, int index);
int ruby_av_player_hls_variant_height(AVPlayer *player, int index);
double ruby_av_player_hls_variant_peak_bitrate(AVPlayer *player, int index);
void ruby_av_player_select_hls_variant(AVPlayer *player, int width, int height, double peakBitrate);
void ruby_av_player_select_hls_auto(AVPlayer *player);
