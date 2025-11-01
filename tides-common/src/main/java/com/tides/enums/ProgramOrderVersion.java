package com.tides.enums;

/**
 * @description: 节目订单枚举
 * @author: 19continue
 **/
public enum ProgramOrderVersion {
    /**
     * 版本
     * */
    V1_VERSION("v1","v1版本",1),
    
    V2_VERSION("v2","v2版本",2),
    
    V21_VERSION("v21","v21版本",21),
   
    V3_VERSION("v3","v3版本",3),
    
    V31_VERSION("v31","v31版本",31),
    
    V4_VERSION("v4","v4版本",4),
    
    V41_VERSION("v41","v41版本",41),
    ;

    private final String version;

    private final String msg;
    
    private final Integer value;

    ProgramOrderVersion(String version, String msg, Integer value) {
        this.version = version;
        this.msg = msg;
        this.value = value;
    }

    public String getVersion() {
        return version;
    }
    

    public String getMsg() {
        return this.msg == null ? "" : this.msg;
    }
    
    public Integer getValue(){
        return value;
    }

    public static boolean isV4Version(Integer value) {
        return V4_VERSION.value.equals(value) || V41_VERSION.value.equals(value);
    }
    

    public static String getMsg(String version) {
        for (ProgramOrderVersion re : ProgramOrderVersion.values()) {
            if (re.version.equals(version)) {
                return re.msg;
            }
        }
        return "";
    }

    public static ProgramOrderVersion getRc(String version) {
        for (ProgramOrderVersion re : ProgramOrderVersion.values()) {
            if (re.version.equals(version)) {
                return re;
            }
        }
        return null;
    }
}
