package org.jeecgframework.web.cgform.enhance;

import org.jeecgframework.core.common.dao.jdbc.JdbcDao;
import org.jeecgframework.core.common.exception.BusinessException;
import org.jeecgframework.core.util.LogUtil;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * Created by zzl_h on 2015/11/24.
 */
@Service("cgformJavaInterDemo")
public class CgformJavaInterDemo implements CgformEnhanceJavaInter {
    @Override

    public void execute(String tableName,Map map) throws BusinessException {
    	LogUtil.info("============调用[java增强]成功!========tableName:"+tableName+"===map==="+map);
    	String status = (String)map.get("status");
    	if (status == "2" || status.equals("2")) {
    		throw new BusinessException("已经提交的订单不能再次上架");
		}
    	String driver = "com.mysql.jdbc.Driver";    
    	String URL = "jdbc:mysql://localhost:3306/jeecg_375";    
        Connection con = null;  
        ResultSet rs = null;  
        PreparedStatement sts = null;  
        PreparedStatement insertst = null;  
        String sql = "SELECT @rowno:=@rowno+1 as rowno,(select IFNULL(max(id),0)+@rowno:=@rowno from wms_stock s)id, f.goodsno, f.number, f.goodsunit, f.goodsname, f.goodssize FROM wms_fetch_sub f ,(select @rowno:=0)a WHERE f.fetch_id = ?;";
        
        try    
        {    
            Class.forName(driver);    
             System.out.println("Connect Successfull.");    
        }    
        catch(java.lang.ClassNotFoundException e)    
        {    
            System.out.println("Cant't load Driver");    
        }    
        try       
        {                                                                                   
            con= DriverManager.getConnection(URL,"root","root");  
            con.setAutoCommit(false);
            
            sts=con.prepareStatement(sql);
            sts.setString(1, (String) map.get("id"));
            rs=sts.executeQuery();  
            if(rs!=null) {  
                ResultSetMetaData rsmd = rs.getMetaData();  
                int countcols = rsmd.getColumnCount();  
                for(int i=1;i<=countcols;i++) {  
                    if(i>1) System.out.print(";");  
                    System.out.print(rsmd.getColumnName(i)+" ");  
                }  
                System.out.println("");  
                while(rs.next()) {  
                	String id = rs.getString("id");
                	String goodsno = rs.getString("goodsno");
                	String number = rs.getString("number");
                	String goodsunit = rs.getString("goodsunit");
                	String goodsname = rs.getString("goodsname");
                	String goodssize = rs.getString("goodssize");
                	
                	String locno = findLoc(goodsno,con);
                    String insertsql = " insert into wms_stock (id,locno,goodsno,goodsname,goodssize,goodsunit,number) values (?,?,?,?,?,?,?)";
                	insertst = con.prepareStatement(insertsql);
                	insertst.setString(1, id);
                	insertst.setString(2, locno);
                	insertst.setString(3, goodsno);
                	insertst.setString(4, goodsname);
                	insertst.setString(5, goodssize);
                	insertst.setString(6, goodsunit);
                	insertst.setString(7, number);
                	
                	insertst.execute();
                	con.commit();
                	
                }  
            }  
            
            
            //System.out.println("Connect Successfull.");    
            System.out.println("ok"); 
            updateFetchStatus((String) map.get("id"),con);
            rs.close();  
            sts.close();  
            insertst.close();
            con.close();  
        }     
        catch(Exception e)    
        {    
            System.out.println("Connect fail:" + e.getMessage());    
        } 
    }
    
    //查找货位
    public String findLoc(String goodsno,Connection con) throws BusinessException {
        ResultSet rst = null;  
        PreparedStatement st = null;  
        PreparedStatement insertst = null;  
        String sql = "select s.locno from wms_stock s where s.goodsno = ? ";//增加顶层标志判断
        String locno = null;
        try{                                                                                   
            st=con.prepareStatement(sql);
            st.setString(1, goodsno);
            rst=st.executeQuery();  
            if(rst!=null) {
                while(rst.next()) { 
                	locno = rst.getString("locno");
                }
            }
            if(locno == null || locno == ""){
            	sql = "select min(l.locno)locno from wms_loc l where not exists (select 1 from wms_stock s where s.locno = l.locno) order by l.loclevel";
            	st.clearParameters();
            	st=con.prepareStatement(sql);
                rst=st.executeQuery();  
                if(rst!=null) {
                    while(rst.next()) { 
                    	
                    	locno = rst.getString("locno");
                    	
                    }
                }
            }
        }catch(Exception e){    
            System.out.println("Connect fail:" + e.getMessage());    
        }finally{
        	try {
				rst.close();
				st.close();  
//				con.close();  
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
        }
        return locno;
    }
  //修改状态
    public void updateFetchStatus(String fetchid,Connection con) throws BusinessException {
        PreparedStatement sta = null;  
        PreparedStatement insertst = null;  
        String sql = "update wms_fetch set status = 2 where id = ? ";
        String locno = null;
        try{                                                                                   
            sta=con.prepareStatement(sql);
            sta.setString(1, fetchid);
            sta.executeUpdate();
            con.commit();
        }catch(Exception e){    
            System.out.println("Connect fail:" + e.getMessage());    
        }finally{
        	try {
				sta.close();  
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
        }
    }
}
