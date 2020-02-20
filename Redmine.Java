package com.redmine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.CustomField;
import com.taskadapter.redmineapi.bean.Issue;

public class Redmine
{
    
    @SuppressWarnings("unused")
    public static void main(String[] args) throws SQLException
    {
        Connection connection = null;
         
        /*if(args.length == 0) {
            System.out.println("Proper Usage is: Arguments needed. i.e. Defect/Support/CR/Feature");
            System.exit(-1);
        }*/
                
        try
        {
           
            String uri = AppConstants.REDMINE_API;
            String apiAccessKey = AppConstants.API_ACESS_KEY;
            String projectKey = AppConstants.PHASE_KEY; //Current Project Phase
            Integer queryId = null; // keep null for getting All records.
            PreparedStatement preparedStatement = null;
            
            // DB Connection
            Class.forName(AppConstants.ORACLE_DRIVER_CLASS);
            connection = DriverManager.getConnection(AppConstants.DB_URL,AppConstants.DB_USER,AppConstants.DB_PWD);
            System.out.println("Connection established... "+connection);
            
            
            RedmineManager mgr = RedmineManagerFactory.createWithApiKey(uri, apiAccessKey);
            if(mgr != null) {
                // delete data
                String deleteQuery = "DELETE FROM CHNG_REQST_TX"; //Main Table
                preparedStatement = connection.prepareStatement(deleteQuery);
                preparedStatement.executeUpdate();
                preparedStatement.close();
                System.out.println("Successfully deleted records from CHNG_REQST_TX Table...");
            }
            System.out.println("RedmineManager is Active, please wait for a while loading data in table...");
            List<Issue> issues;
            issues = mgr.getIssueManager().getIssues(projectKey, queryId);
            //System.out.println(issues.toString());
            int count = 0;
            for (Issue issue : issues)
            {
                //Only Feature + CR for Change Request Form(PDF)
                /*if(!(issue.getTracker().getName().equalsIgnoreCase("Support") || issue.getTracker().getName().equalsIgnoreCase("Defect"))) {*/
                //args[0] = "CR";
                //if(issue.getTracker().getName().equalsIgnoreCase("CR")){
                    Date dueDate = issue.getDueDate();
                    Date createdDate = issue.getCreatedOn();
                    String description = issue.getDescription();
                    int redmineId = issue.getId();
                    String priority = issue.getPriorityText();
                    String tracker = issue.getTracker().getName();
                    String project = issue.getProjectName();
                    String module = "";
                    if(issue.getCustomFieldByName("Module") != null)
                        module = issue.getCustomFieldByName("Module").getValue();

                    Date goLive = null;
                    CustomField goLiveCustomField = issue.getCustomFieldByName("Go Live");
                    if (goLiveCustomField != null)
                    {
                        String goLiveString = issue.getCustomFieldByName("Go Live").getValue();
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        if (goLiveString != null && !goLiveString.equalsIgnoreCase(""))
                        {
                            goLive = formatter.parse(goLiveString);
                        }

                    }

                    ResultSet rs = null;
                    if (!module.equalsIgnoreCase(""))
                    {
                        // fetch data from master table
                        String fetchData = "SELECT CHNG_REQST_OFCL_ID, BNK_OFCL_NAME, SYS_OFCL_NAME||'/'||SYS_BU_OFCL_NAME, BU_OFCL_NAME FROM CHNG_REQST_OFCL_MST WHERE MODULE_DESC=?1";
                        preparedStatement = connection.prepareStatement(fetchData);
                        preparedStatement.setString(1, module);
                        rs = preparedStatement.executeQuery();
                    }

                    String chngReqstOfclId = null;
                    String bnkOfclName1 = null;
                    String sysOfclNameAndSysBuOfclName3_4 = null;
                    String buOfclName2 = null;
                    while (rs != null && rs.next())
                    {
                        chngReqstOfclId = rs.getString("CHNG_REQST_OFCL_ID");
                        bnkOfclName1 = rs.getString("BNK_OFCL_NAME");
                        sysOfclNameAndSysBuOfclName3_4 = rs.getString("SYS_OFCL_NAME||'/'||SYS_BU_OFCL_NAME");
                        buOfclName2 = rs.getString("BU_OFCL_NAME");
                    }
                    if (rs != null)
                    {
                        rs.close();
                        preparedStatement.close();
                    }

                    // insert data
                    String insertQuery = "insert into CHNG_REQST_TX values(SEQ_CHNG_REQST_ID.NextVAL, 'LLMS/2019-20/',?1,'',?2,?3,'LLMS',?4,'',?5,?6,'Permanent',?7,'Application',?8,'Additional/ New Functionality in LLMS as requested by BU','LLMS Users','Not Applicable. Part of LLMS','None',?9,'',?10,'','','',?11,'','','','','','','In house developed','YES','YES','','','Testing Carried out in UAT enviroment','','','','','','','','','','','','','','','','','','','','',?12,'','','','','','','','','')";
                    preparedStatement = connection.prepareStatement(insertQuery);
                    preparedStatement.setInt(1, redmineId);
                    preparedStatement.setString(2, project);
                    preparedStatement.setString(3, chngReqstOfclId);
                    preparedStatement.setTimestamp(4, null);
                    if (createdDate != null)
                    {
                        preparedStatement.setTimestamp(4, new Timestamp(createdDate.getTime()));
                    }
                    preparedStatement.setString(5, priority);
                    preparedStatement.setString(6, tracker);
                    preparedStatement.setTimestamp(7, null);
                    if (dueDate != null)
                    {
                        preparedStatement.setTimestamp(7, new Timestamp(dueDate.getTime()));
                    }
                    preparedStatement.setString(8, description);
                    preparedStatement.setString(9, bnkOfclName1);
                    preparedStatement.setString(10, sysOfclNameAndSysBuOfclName3_4);
                    preparedStatement.setString(11, buOfclName2);
                    preparedStatement.setTimestamp(12, null);
                    if (goLive != null)
                    {
                        preparedStatement.setTimestamp(12, new Timestamp(goLive.getTime()));
                    }
                    System.out.println(++count +" - Redmine Id : "+redmineId);
                    try{
                        preparedStatement.executeUpdate();
                        
                    }catch(Exception e){
                        System.out.println("SQL Exception for RedmineId - "+redmineId+"\n"+e.getMessage());
                    }
                    preparedStatement.close();
                
                    
               // }
            /*}*/
          }   
        } catch (Exception e)
        {
            e.printStackTrace();
            
        } finally {
            
            System.out.println("success");
            connection.close();
        }
    }

}
