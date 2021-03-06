package org.com.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.com.DbMgr;
import org.com.model.LaserHeadBean;
import org.com.model.ScenarioBean;

public class ScenarioDAO implements D2ctsDao<ScenarioBean>{
	private static final Logger log = Logger.getLogger(ScenarioDAO.class);

	private static final String LOAD_QUERY = "SELECT ID, NAME, DATE_REC, TERMINAL, FILE FROM SCENARIO";
	private static final String INSERT_SCENARIO_QUERY = "INSERT INTO SCENARIO (NAME, DATE_REC, TERMINAL, FILE) VALUES (?, ?, ?, ?)";
	private static final String DELETE_QUERY = "DELETE FROM SCENARIO WHERE ID = ?";
	
	
	private static ScenarioDAO instance;

	Map<Integer, ScenarioBean> beans;

	PreparedStatement scenariosStatement;
	PreparedStatement insertStatement;
	PreparedStatement deleteStatement;
	
	public static final ScenarioDAO getInstance(){
		if(instance == null){
			instance = new ScenarioDAO();
		}
		return instance;
	}

	private  ScenarioDAO(){
		beans = new HashMap<>();
		try{
			load();
		} catch (SQLException e){
			log.fatal(e.getMessage(),e);
		}
	}

	@Override
	public void close() throws SQLException{
		if(scenariosStatement != null){
			scenariosStatement.close();
		}
		if(insertStatement != null){
			insertStatement.close();
		}
		if(deleteStatement != null){
			deleteStatement.close();
		}
		instance = null;
		log.info("ScenarioDAO closed.");
	}

	@Override
	public int size(){
		return beans.size();
	}

	@Override
	public void load() throws SQLException{
		Connection c = DbMgr.getInstance().getConnection();
		scenariosStatement = c.prepareStatement(getLoadQuery());
		ResultSet rs = scenariosStatement.executeQuery();
		while(rs.next()){
			ScenarioBean bean = new ScenarioBean();
			bean.setId(rs.getInt("ID"));
			bean.setName(rs.getString("NAME"));
			bean.setDate_rec(rs.getTimestamp("DATE_REC"));
			bean.setTerminal(rs.getInt("TERMINAL"));
			bean.setFile(rs.getString("FILE"));

			beans.put(bean.getId(), bean);
		}
		if(rs != null){
			rs.close();
		}
	}

	@Override
	public String getLoadQuery(){
		return ScenarioDAO.LOAD_QUERY;
	}

	@Override
	public Iterator<ScenarioBean> iterator() {
		return beans.values().iterator();
	}

	public String[] getColumnsName(){
		return new String[]{"ID","NAME","DATE_REC","FILE"};
	}


	@Override
	public int insert(ScenarioBean bean) throws SQLException {
		if (insertStatement == null) {
			Connection c = DbMgr.getInstance().getConnection();
			insertStatement = c.prepareStatement(INSERT_SCENARIO_QUERY, Statement.RETURN_GENERATED_KEYS);
		}
		bean.setDate_rec(new Timestamp(System.currentTimeMillis()));
		insertStatement.setString(1, bean.getName());
		insertStatement.setTimestamp(2, new Timestamp(bean.getDate_rec().getTime()));
		insertStatement.setInt(3, bean.getTerminal());
		insertStatement.setString(4, bean.getFile());
		int res = insertStatement.executeUpdate();

		ResultSet rs = insertStatement.getGeneratedKeys();
		if (rs != null && rs.first()) {
			// récupère l'id généré
			bean.setId(rs.getInt(1));
		}

		if (rs != null) {
			rs.close();
		}
		
		//Add new bean into collection.
		beans.put(bean.getId(), bean);
		
		//Insert new LaserHeads for this brand new scenario
		insertDefaultLH(bean);
		
		return res;
	}

	private void insertDefaultLH (ScenarioBean bean) throws SQLException {
		for(LaserHeadBean lh : LaserHeadBean.getDefaultHeads()){
			lh.setScenario(bean.getId());
			LaserHeadDAO.getInstance(bean.getId()).insert(lh);
			DbMgr.getInstance().getConnection().commit();
		}
	}
	
	public ScenarioBean getScenario(Integer id) {
		return beans.get(id);
	}
	
	public TreeSet<ScenarioBean> getScenariosOfAKind(String name){
		TreeSet<ScenarioBean> result = new TreeSet<>();
		for(ScenarioBean bean : beans.values()){
			if(bean.getName().startsWith(name)){
				result.add(bean);
			}
		}
		return result;
	}

	public int delete(ScenarioBean scenario) throws SQLException{
		if(deleteStatement == null){
			deleteStatement = DbMgr.getInstance().getConnection().prepareStatement(DELETE_QUERY);
		}
		deleteStatement.setInt(1, scenario.getId());
		int result = deleteStatement.executeUpdate();
		return result;
	}
}
