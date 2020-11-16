package com.capable.physiciandss;

import com.capable.physiciandss.model.get.ItemData;
import com.capable.physiciandss.model.get.PlanTask;
import com.capable.physiciandss.requests.RequestServiceWS;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class PhysiciandssApplication {


    public static void main(String[] args) {
        SpringApplication.run(PhysiciandssApplication.class, args);
        RequestServiceWS RS = new RequestServiceWS();

        RS.getPathway(false).subscribe(pathways -> {
            System.out.println(Arrays.toString(pathways));
            String pathwayID = pathways[0].getId();
            RS.postEnact(pathwayID, "0").subscribe(enact -> {
                System.out.println(enact);
                RS.getConnect(enact.getEnactmentid()).subscribe(connect -> {
                    System.out.println(connect);
                    RS.getPlanTasks("in_progress", connect.getDresessionid()).subscribe(tasks -> {
                        for (PlanTask task : tasks
                        ) {
                            System.out.println(task);
                            if (task.getType().equals("enquiry")) {
                                RS.getData(task.getName(), connect.getDresessionid()).subscribe(data -> {
                                    if (data.length == 0) {
                                        System.out.println("Task nie ma data values!");
                                    }
                                    for (ItemData d : data
                                    ) {
                                        System.out.println(d);
                                        RS.putDataValue(d.getName(), "2", connect.getDresessionid()).subscribe(res -> {
                                            System.out.println("Dodanie data value:" + res.isSuccess());
                                        });
                                    }

                                });

                                //RS.putConfirmTask(task.getName(), connect.getDresessionid()).subscribe(ct -> System.out.println(ct.getState()));
                            }

                        }
                    });
                    RS.putEnactmentDelete(enact.getEnactmentid(), connect.getDresessionid()).subscribe(st -> System.out.println("Usuniecie enact:" + st.getDeleted()));
                });
            });
        });
//        System.out.println(Arrays.toString(enactments));
//        System.out.println(Arrays.toString(new RequestServiceWS().getPathway()));
//        Connect connection = new RequestServiceWS().getConnection(enactments[0].getId());
//        System.out.println(connection);
//        System.out.println(Arrays.toString(new RequestServiceWS().getData(connection.getDresessionid())));
    }
}
