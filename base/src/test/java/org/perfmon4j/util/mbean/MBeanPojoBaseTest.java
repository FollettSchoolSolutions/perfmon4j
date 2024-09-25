package org.perfmon4j.util.mbean;

import org.mockito.Mockito;
import org.perfmon4j.instrument.snapshot.JavassistSnapShotGenerator;
import org.perfmon4j.instrument.snapshot.SnapShotGenerator;

import junit.framework.TestCase;

public class MBeanPojoBaseTest extends TestCase {
	private final SnapShotGenerator snapShotGenerator = new JavassistSnapShotGenerator();
	
	public void testGetEffectiveClassName()throws Exception {
		MBeanInstance mBeanInstance = Mockito.mock(MBeanInstance.class);
		Mockito.when(mBeanInstance.getDatumDefinition()).thenReturn(null);
		Mockito.when(mBeanInstance.getName()).thenReturn("JVMMemoryPool");
	
		Class<MBeanPojoBase> pojoClass = snapShotGenerator.generatePOJOClassForMBeanInstance(mBeanInstance);
		MBeanPojoBase pojo = MBeanPojoBase.invokeConstructor(pojoClass, mBeanInstance);
		assertEquals("JVMMemoryPool", pojo.getEffectiveClassName());
	}
}
