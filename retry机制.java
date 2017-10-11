//方法一：
private OpenRetryMessageRunnable m_ScouterOff = new OpenRetryMessageRunnable("ADAS_24_T1", 5 * 60 * 1000, 6);

private void openRetryMessage(OpenRetryMessageRunnable runnable) {
		if(null != m_Handler){
			m_Handler.removeCallbacks(runnable);
			runnable.resetTask();
			m_Handler.post(runnable);
		}
}

private class OpenRetryMessageRunnable implements Runnable {
	private String m_MessageID;
		private int interval = 0;
		private int times = 0;
		private int defInterval=0;
		private int degTimes=0;

		public OpenRetryMessageRunnable(String messageID, int interval, int times) {
			m_MessageID = messageID;
			this.interval = interval;
			this.times = times;
			this.defInterval=interval;
			this.degTimes=times;
		}
		
		public void resetTask(){
			this.interval=defInterval;
			this.times = degTimes;
		}

		@Override
		public void run() {
			try {
				Log.d(Log.LOGID_ADAS, TAG, "OpenRetryMessageRunnable:m_MessageID=" + m_MessageID);
				boolean openResult = false;
				IRemoteSysService m_Service = AvApp.getInstance().getRemoteSysService();

				if (null == m_Service) {
					Log.d(Log.LOGID_ADAS, TAG, "OpenRetryMessage failed m_Service==null");
				} else if (!m_Service.isMessageOpened(m_MessageID)) {
					if ("ADAS_24_T1".equals(m_MessageID)) {
						CONNECTION_STATUS connectStatus = ADASController.getInstance().getCurrentConnectionStatus();
						Log.d(Log.LOGID_ADAS, TAG, "getCurrentConnectionStatus:connectStatus=" + connectStatus);
						if (CONNECTION_STATUS.DISCONNECTED == connectStatus && m_isMessageAllowedtoOpen) {
							openResult = m_Service.openMessage(m_MessageID);
							if (openResult) {
								m_isMessageAllowedtoOpen = false;
							}
						} else {
							openResult = true;// end runnable
						}
					} else {
						openResult = m_Service.openMessage(m_MessageID);
					}
				} else {
					Log.d(Log.LOGID_ADAS, TAG, "OpenRetryMessage failed m_MessageID=" + m_MessageID);
				}

				if (!openResult) {
					if (times > 0) {
						m_Handler.postDelayed(this, interval);
						times--;
					}
				}
			} catch (ADASException e) {
				Log.d(Log.LOGID_ADAS, TAG, "getCurrentConnectionStatus exception");
			} catch (RemoteException e) {
				Log.d(Log.LOGID_ADAS, TAG, "OpenRetryMessageRunnable exception,m_MessageID=" + m_MessageID);
			}
		}
}

//方法二：
private int count = 0;
    private Timer m_timer = null;
    private TimerTask m_task = null;
    //retry 3次，每次间隔8s
    private void startTimer(){
        m_timer = new Timer();
        m_task = new TimerTask() {
            @Override
            public void run() {
                if(isUpdating()){
                   ++count;
                   if(count == 3){
                       Log.d(TAG, "startTimer >>> count=3!" );
                       //do something
                       removeTimer();
                   }
                }else{
                    if(OPEN_MESSAGE_SUCCESS == openMessage("message")){
                        Log.d(TAG, "startTimer >>> message has opened!" );
                    }
                    removeTimer();
                }
            }
        };
        if (m_task != null && m_timer != null) {
            m_timer.schedule(m_task, 8000, 8000);
        }
    }

    private void removeTimer(){
        if (m_task != null) {
            m_task.cancel();
            m_task = null;
        }
        if (m_timer != null) {
            m_timer.cancel();
            m_timer = null;
        }
        if(count != 0){
            count = 0;
        }
    }

    //使用此retry时，只需调用startTimer()即可