package com.capable.physiciandss.schedulers;

import com.capable.physiciandss.flow.ProcessFlow;
import com.capable.physiciandss.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Klasa zawierające metody wykonywane w określonych odstępach czasu
 */
@Component
public class ScheduledTasks extends com.capable.physiciandss.flow.ProcessFlow {


    protected static final Logger log =
            LoggerFactory.getLogger(ScheduledTasks.class);

    /**
     * Metoda wywołująca process-flow aplikacji co 10 sekund
     */
    @Scheduled(fixedRate = 10000)
    @Async
    public void checkForDataToProcess() {
        log.info(Constants.SCHEDULER_TASK_INFO);
        new ProcessFlow().startProcessFlow();
    }


}
