# EarthQuakePlayer2

EarthqaukePlayer2는 그동안 학습한 내용을 기반으로 개발한 공부용 프로젝트 입니다.

해당 소스코드는 필요하시면 사용가능 합니다.

UAMP (https://github.com/android/uamp) 와 DraggablePanel(https://github.com/pedrovgs/DraggablePanel)을 참고하였습니다.

본 프로젝트로 아래와 같은 기능을 학습할 수 있습니다.
MediaSession + ExoPlayer + RxKotlin + Coroutine + ViewDragHelper 등을 학습할 수 있습니다.

1. Music + Video 플레이
- Video플레이는 Youtubelayout을 구현해서 기능처리 하였습니다. (ExoPlayer를 사용하였습니다.)
- Video플레이어의 Youtubelayout기능은 ViewDragHelper를 사용하여 구현하였습니다.
- Music플레이어는 MediaSession + ExoPlayer를 사용하였습니다. 구현의 편의를 위하여 MediaSessionConnector클래스를 사용하였습니다.
- Music플레이어에서 Audiofocus는 사용하지 않았습니다.
- SongFragment에서 사용한 SubscriptionCallback은 학습용으로 사용하였습니다. 기능상 DB연동 처리를 따로 하는것이 더욱 편한것 같습니다.
- DB연동 및 내부적으로 시간이 걸리는 처리는 Coroutine을 사용하였습니다. (동기화를 위하여 withContext와 suspend조합을 사용하였습니다.)
- Music플레이어에서 ACTION_SKIP_TO_PREVIOUS와 ACTION_SKIP_TO_NEXT처리를 위해서 MediaSessionConnector.QueueNavigator를 사용하였습니다.

2. RxKotlin
- EvnetBus대용으로 RxBus를 구현하여 사용하였습니다.
- 퍼미션처리에 사용하였습니다. (MainActivity에서 사용함)

3. Fragment stack처리
- addToBackStack와 popBackStackImmediate를 사용하여 fragment 스택관리 처리를 하였습니다.

4. Viewpager2 사용
- AnimatedBottomBar(https://github.com/Droppers/AnimatedBottomBar)를 처리하기 위해서 안드로이드 신기술인 viewpager2를 사용하였습니다.

5. 기타
- 혹시나 놓친 부분이 있으면 정리해 놓을께요.

English Translation

EarthqaukePlayer2 is a learning project which is developed base on my study

This source code is the copyleft and you can freely use it with modified or not

This code is mostly refer to UAMP (https://github.com/android/uamp) and DraggablePanel(https://github.com/pedrovgs/DraggablePanel)
of cause additionaly google android developer site

You can learn as below with this project
MediaSession + ExoPlayer + RxKotlin + Coroutine + ViewDragHelper 등을 학습할 수 있습니다.

1. Music + Video 플레이
- Video플레이는 Youtubelayout을 구현해서 기능처리 하였습니다. (ExoPlayer를 사용하였습니다.)
- Video플레이어의 Youtubelayout기능은 ViewDragHelper를 사용하여 구현하였습니다.
- Music플레이어는 MediaSession + ExoPlayer를 사용하였습니다. 구현의 편의를 위하여 MediaSessionConnector클래스를 사용하였습니다.
- Music플레이어에서 Audiofocus는 사용하지 않았습니다.
- SongFragment에서 사용한 SubscriptionCallback은 학습용으로 사용하였습니다. 기능상 DB연동 처리를 따로 하는것이 더욱 편한것 같습니다.
- DB연동 및 내부적으로 시간이 걸리는 처리는 Coroutine을 사용하였습니다. (동기화를 위하여 withContext와 suspend조합을 사용하였습니다.)
- Music플레이어에서 ACTION_SKIP_TO_PREVIOUS와 ACTION_SKIP_TO_NEXT처리를 위해서 MediaSessionConnector.QueueNavigator를 사용하였습니다.

2. RxKotlin
- EvnetBus대용으로 RxBus를 구현하여 사용하였습니다.
- 퍼미션처리에 사용하였습니다. (MainActivity에서 사용함)

3. Fragment stack처리
- addToBackStack와 popBackStackImmediate를 사용하여 fragment 스택관리 처리를 하였습니다.

4. Viewpager2 사용
- AnimatedBottomBar(https://github.com/Droppers/AnimatedBottomBar)를 처리하기 위해서 안드로이드 신기술인 viewpager2를 사용하였습니다.

5. 기타
- 혹시나 놓친 부분이 있으면 정리해 놓을께요.
