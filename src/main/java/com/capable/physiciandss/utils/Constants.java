package com.capable.physiciandss.utils;

public class Constants {
    //public static final String HAPI_BASE_URL = "http://10.131.46.196:8080/baseR4/";
    public static final String HAPI_BASE_URL = "http://localhost:9000/baseR4/";
    public static final String GOCOM_BASE_URL = "localhost:9000";


    public static final String X_APIKEY = "h8MUoJLm2W8iwN5KVdpi2SxLFV4rZUoL";
    public static final String DEONTICS_BASE_URL = "https://cap-dev.deontics.com/dwe/a/capable-vc";
    public static final String DRE_API_URL = "/dreapi";
    public static final String PRS_API_URL = "/prsapi";

    public static final String REQUEST_SUCCEDED_MESSAGE = " succeded!";
    public static final String REQUEST_FAILED_MESSAGE = " failed with error code: ";

    public static final String SCHEDULER_TASK_INFO = "Checking if there is any data to process";
    public static final String SCHEDULER_TASK_BAD_PAYLOAD_TYPE = "Wrong payload type";
    public static final String SCHEDULER_TASK_BAD_DEONTIC_TASKS_TYPE = "Wrong deontic task's type";

    public static final String DEONTICS_IN_PROGRESS_STATUS = "in_progress";
    public static final String DEONTICS_ENQUIRY_TASK_TYPE = "enquiry";
    public static final String DEONTICS_ACTION_TASK_TYPE = "action";

    public static final String META_GUIDELINE_NAME = "project_ph_meta_guideline";
    public static final String HAPI_DATETIMETYPE_FORMAT_MR = "yyyy-MM-dd";
    public static final String HAPI_DATETIMETYPE_FORMAT_OBS = "yyyy-MM-dd'T'hh:mm:ss+zz:zz";

    public static final String SNOMED_CODING_HAPI = "http://snomed.info/sct";
    public static final String SNOMED_CODING_DEONTICS = "SCT";
    public static final String IMMUNOTHERAPY_CODE = "64644003";
    public static final String COMPLICATED_DIARRHEA_CODE = "409587002";
    public static final String PERSISTENT_DIARRHEA_CODE = "236071009";
    public static final String SUNITIB_CODE = "421192001";
    public static final String NIVOLUMAB_CODE = "704191007";
    public static final String DIARRHEA_SYMPTOMS_CODE = "386661006";
    public static final String STRONG_DIARRHEA_SYMPTOMS_CODE = "62315008";
}
