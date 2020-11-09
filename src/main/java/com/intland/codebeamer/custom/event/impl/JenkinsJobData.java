package com.intland.codebeamer.custom.event.impl;

import java.io.Serializable;
import java.util.List;

public class JenkinsJobData implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<JenkinsParameter> parameter;

	public JenkinsJobData() {
		super();
	}

	public JenkinsJobData(List<JenkinsParameter> parameter) {
		super();
		this.parameter = parameter;
	}

	public List<JenkinsParameter> getParameter() {
		return parameter;
	}

	public void setParameter(List<JenkinsParameter> parameter) {
		this.parameter = parameter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parameter == null) ? 0 : parameter.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JenkinsJobData other = (JenkinsJobData) obj;
		if (parameter == null) {
			if (other.parameter != null)
				return false;
		} else if (!parameter.equals(other.parameter))
			return false;
		return true;
	}

}