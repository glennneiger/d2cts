package org.com.model.scheduling;

import java.util.ArrayList;
import java.util.List;

import org.scheduling.MissionSchedulerEvalParameters;

public enum RandomParametersBean implements SchedulingParametersBeanInterface {
	L("L"), T("T"), E("E");

	private String name;
	private Double value;

	private Integer sqlId;

	private RandomParametersBean(String name) {
		this.name = name;
		this.value = null;
	}

	@Override
	public void setValue(Object o) {
		if (o instanceof String) {
			this.value = Double.parseDouble((String) o);
		} else {
			this.value = (Double) o;
		}
	}

	@Override
	public Double getValueAsDouble() {
		return this.value;
	}

	@Override
	public String getValueAsString() {
		return this.value == null ? "null" : this.value.toString();
	}

	@Override
	public Integer getSQLID() {
		return this.sqlId;
	}

	@Override
	public void setSQLID(Integer id) {
		this.sqlId = id;
	}

	@Override
	public String toString() {
		return this.name + "=" + getValueAsString();
	}

	public static MissionSchedulerEvalParameters getEvalParameters() {
		return new MissionSchedulerEvalParameters(T.value, L.value, E.value);
	}

	public static RandomParametersBean get(String name) {
		for (RandomParametersBean param : RandomParametersBean.values()) {
			if (param.name.equalsIgnoreCase(name))
				return param;
		}
		return null;
	}

	public static ParameterType[] types() {
		ParameterType[] t = new ParameterType[values().length];
		for (int i = 0; i < values().length; i++) {
			t[i] = ParameterType.DOUBLE;
		}
		return t;
	}

	@Override
	public Integer getValueAsInteger() {
		return this.value.intValue();
	}

	public static String[] names() {
		String[] t = new String[RandomParametersBean.values().length];
		int i = 0;
		for (RandomParametersBean p : RandomParametersBean.values())
			t[i++] = p.name;

		return t;
	}

	public static List<ParameterBean> getAll() {
		List<ParameterBean> l = new ArrayList<>(values().length);
		for (RandomParametersBean b : values()) {
			l.add(b.getParameter());
		}
		return l;
	}

	@Override
	public ParameterBean getParameter() {
		ParameterBean p = new ParameterBean(name, ParameterType.DOUBLE);
		p.setValue(value);
		p.setSQLID(getSQLID());
		return p;
	}

}
