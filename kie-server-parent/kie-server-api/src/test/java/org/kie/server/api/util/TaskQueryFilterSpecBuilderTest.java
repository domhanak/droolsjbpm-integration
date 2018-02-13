package org.kie.server.api.util;

import static org.assertj.core.api.Assertions.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.kie.server.api.model.definition.QueryParam;
import org.kie.server.api.model.definition.TaskField;
import org.kie.server.api.model.definition.TaskQueryFilterSpec;

public class TaskQueryFilterSpecBuilderTest {
	
	@Test
	public void testGetEqualsTo() {
		TaskQueryFilterSpec filterSpec = new TaskQueryFilterSpecBuilder().equalsTo(TaskField.PROCESSID, "test-process").get();
		
		QueryParam[] params = filterSpec.getParameters();
		assertThat(params.length).isEqualTo(1);
		
		QueryParam param = params[0];
		assertThat(param.getColumn()).isEqualTo(TaskField.PROCESSID.toString());
		assertThat(param.getOperator()).isEqualTo("EQUALS_TO");
		assertThat(param.getValue().stream().findFirst().get()).isEqualTo("test-process");
	}
	
	@Test
	public void testGetBetween() {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date from = null;
		Date to = null;
		try {
		   from = sdf.parse("2017-05-10");
		   to = sdf.parse("2017-05-14");
		} catch (ParseException e) {
		   e.printStackTrace();
		}
		
		TaskQueryFilterSpec filterSpec = new TaskQueryFilterSpecBuilder().between(TaskField.CREATEDON, from, to).get();
		
		QueryParam[] params = filterSpec.getParameters();
		assertThat(params.length).isEqualTo(1);
		
		QueryParam param = params[0];
		assertThat(param.getColumn()).isEqualTo(TaskField.CREATEDON.toString());
		assertThat(param.getOperator()).isEqualTo("BETWEEN");
		List<?> values = param.getValue();
		assertThat(values).hasSize(2);
		assertThat(values.get(0)).isEqualTo(from);
		assertThat(values.get(1)).isEqualTo(to);
	}
	
	@Test
	public void testGetEqualsToAndBetween() {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Date from = null;
		Date to = null;
		try {
		   from = sdf.parse("2017-05-10");
		   to = sdf.parse("2017-05-14");
		} catch (ParseException e) {
		   e.printStackTrace();
		}
		
		TaskQueryFilterSpec filterSpec = new TaskQueryFilterSpecBuilder().equalsTo(TaskField.PROCESSID, "test-process").between(TaskField.CREATEDON, from, to).get();
		
		QueryParam[] params = filterSpec.getParameters();
		assertThat(params.length).isEqualTo(2);
		
		QueryParam paramEqualsTo = params[0];
		assertThat(paramEqualsTo.getColumn()).isEqualTo(TaskField.PROCESSID.toString());
		assertThat(paramEqualsTo.getOperator()).isEqualTo("EQUALS_TO");
		assertThat(paramEqualsTo.getValue().stream().findFirst().get()).isEqualTo("test-process");
		
		QueryParam paramBetween = params[1];
		assertThat(paramBetween.getColumn()).isEqualTo(TaskField.CREATEDON.toString());
		assertThat(paramBetween.getOperator()).isEqualTo("BETWEEN");
		List<?> values = paramBetween.getValue();
		assertThat(values).hasSize(2);
		assertThat(values.get(0)).isEqualTo(from);
		assertThat(values.get(1)).isEqualTo(to);
	}
		
}
