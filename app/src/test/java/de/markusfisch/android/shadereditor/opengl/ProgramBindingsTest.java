package de.markusfisch.android.shadereditor.opengl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class ProgramBindingsTest {
	@Test
	public void rowMajorMatricesAreConvertedWithoutChangingTheSource() {
		var bindings = new ProgramBindings(new GlProgram(1));
		float[] rotation = {1, 2, 3, 4, 5, 6, 7, 8, 9};
		float[] inclination = {9, 8, 7, 6, 5, 4, 3, 2, 1};
		bindings.setMatrix3("rotation", true, rotation);
		bindings.setMatrix3("inclination", true, inclination);

		var uniforms = bindings.getUniforms().iterator();
		var rotationUniform = uniforms.next();
		var inclinationUniform = uniforms.next();
		assertFalse(rotationUniform.transpose);
		assertFalse(inclinationUniform.transpose);
		assertArrayEquals(new float[]{1, 4, 7, 2, 5, 8, 3, 6, 9},
				rotationUniform.values, 0f);
		assertArrayEquals(new float[]{9, 6, 3, 8, 5, 2, 7, 4, 1},
				inclinationUniform.values, 0f);
		assertArrayEquals(new float[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, rotation, 0f);

		float[] converted = rotationUniform.values;
		bindings.clear();
		bindings.setMatrix3("rotation", false, rotation);
		assertSame(rotation, rotationUniform.values);
		bindings.setMatrix3("rotation", true, inclination);
		assertSame(converted, rotationUniform.values);
		assertArrayEquals(inclinationUniform.values, rotationUniform.values, 0f);
	}
}
