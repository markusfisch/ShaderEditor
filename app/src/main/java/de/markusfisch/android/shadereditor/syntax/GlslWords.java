package de.markusfisch.android.shadereditor.syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class GlslWords {
	private static final String[] DIRECTIVES = {
			"define", "elif", "else", "endif", "error", "if", "ifdef",
			"ifndef", "include", "line", "pragma", "undef", "version"};
	// Room for the words below, which add() keeps sorted.
	private static final String[] WORDS = new String[400];
	private static final Style[] STYLES = new Style[WORDS.length];
	private static int count;

	static {
		add(Style.PRIMITIVE, "const", "bool", "float", "int", "uint",
				"double");
		add(Style.TYPE_VECTOR, "bvec2", "bvec3", "bvec4", "ivec2", "ivec3",
				"ivec4", "uvec2", "uvec3", "uvec4", "vec2", "vec3", "vec4");
		add(Style.TYPE_MATRIX, "mat2", "mat3", "mat4", "mat2x2", "mat2x3",
				"mat2x4", "mat3x2", "mat3x3", "mat3x4", "mat4x2", "mat4x3",
				"mat4x4", "dvec2", "dvec3", "dvec4", "dmat2", "dmat3", "dmat4",
				"dmat2x2", "dmat2x3", "dmat2x4", "dmat3x2", "dmat3x3",
				"dmat3x4", "dmat4x2", "dmat4x3", "dmat4x4");
		add(Style.TYPE_QUALIFIER, "centroid", "in", "out", "inout",
				"uniform", "patch", "sample", "buffer", "shared", "coherent",
				"volatile", "restrict", "readonly", "writeonly",
				"noperspective", "flat", "smooth", "layout");
		add(Style.ATOMIC_COUNTER, "atomic_uint");
		add(Style.TYPE_SAMPLER, "sampler2D", "sampler3D", "samplerCube",
				"sampler2DShadow", "samplerCubeShadow", "sampler2DArray",
				"sampler2DArrayShadow", "isampler2D", "isampler3D",
				"isamplerCube", "isampler2DArray", "usampler2D", "usampler3D",
				"usamplerCube", "usampler2DArray", "sampler1D",
				"sampler1DShadow", "sampler1DArray", "sampler1DArrayShadow",
				"isampler1D", "isampler1DArray", "usampler1D",
				"usampler1DArray", "sampler2DRect", "sampler2DRectShadow",
				"isampler2DRect", "usampler2DRect", "samplerbuffer",
				"isamplerbuffer", "usamplerbuffer", "samplerCubeArray",
				"samplerCubeArrayShadow", "isamplerCubeArray",
				"usamplerCubeArray", "sampler2DMS", "isampler2DMS",
				"usampler2DMS", "sampler2DMSArray", "isampler2DMSArray",
				"usampler2DMSArray");
		add(Style.TYPE_IMAGE, "image2D", "iimage2D", "uimage2D", "image3D",
				"iimage3D", "uimage3D", "imageCube", "iimageCube",
				"uimageCube", "imagebuffer", "iimagebuffer", "uimagebuffer",
				"image2DArray", "iimage2DArray", "uimage2DArray",
				"imageCubeArray", "iimageCubeArray", "uimageCubeArray",
				"image1D", "iimage1D", "uimage1D", "image1DArray",
				"iimage1DArray", "image2DRect", "iimage2DRect", "uimage2DRect",
				"image2DMS", "iimage2DMS", "uimage2DMS", "image2DMSArray",
				"iimage2DMSArray", "uimage2DMSArray");
		add(Style.KEYWORD, "struct", "void");
		add(Style.CONTROL_FLOW, "while", "break", "continue", "do", "else",
				"for", "if", "discard", "return", "switch", "case", "default",
				"subroutine");
		add(Style.PRECISION, "invariant", "precise", "highp", "mediump",
				"lowp", "precision");
		add(Style.BUILTIN_FUNCTION, "abs", "acos", "acosh", "all", "any",
				"asin", "asinh", "atan", "atanh", "atomicAdd", "atomicAnd",
				"atomicCompSwap", "atomicCounter", "atomicCounterDecrement",
				"atomicCounterIncrement", "atomicExchange", "atomicMax",
				"atomicMin", "atomicOr", "atomicXor", "barrier", "bitCount",
				"bitfieldExtract", "bitfieldInsert", "bitfieldReverse", "ceil",
				"clamp", "cos", "cosh", "cross", "degrees", "determinant",
				"dFdx", "dFdxCoarse", "dFdxFine", "dFdy", "dFdyCoarse",
				"dFdyFine", "distance", "dot", "EmitStreamVertex",
				"EmitVertex", "EndPrimitive", "EndStreamPrimitive", "equal",
				"exp", "exp2", "faceforward", "findLSB", "findMSB",
				"floatBitsToInt", "floatBitsToUint", "floor", "fma", "fract",
				"frexp", "fwidth", "fwidthCoarse", "fwidthFine",
				"gl_ClipDistance", "gl_CullDistance", "gl_FragCoord",
				"gl_FragDepth", "gl_FrontFacing", "gl_GlobalInvocationID",
				"gl_HelperInvocation", "gl_InstanceID", "gl_InvocationID",
				"gl_Layer", "gl_LocalInvocationID", "gl_LocalInvocationIndex",
				"gl_NumSamples", "gl_NumWorkGroups", "gl_PatchVerticesIn",
				"gl_PointCoord", "gl_PointSize", "gl_Position",
				"gl_PrimitiveID", "gl_PrimitiveIDIn", "gl_SampleID",
				"gl_SampleMask", "gl_SampleMaskIn", "gl_SamplePosition",
				"gl_TessCoord", "gl_TessLevelInner", "gl_TessLevelOuter",
				"gl_VertexID", "gl_ViewportIndex", "gl_WorkGroupID",
				"gl_WorkGroupSize", "greaterThan", "greaterThanEqual",
				"groupMemoryBarrier", "imageAtomicAdd", "imageAtomicAnd",
				"imageAtomicCompSwap", "imageAtomicExchange", "imageAtomicMax",
				"imageAtomicMin", "imageAtomicOr", "imageAtomicXor",
				"imageLoad", "imageSamples", "imageSize", "imageStore",
				"imulExtended", "intBitsToFloat", "interpolateAtCentroid",
				"interpolateAtOffset", "interpolateAtSample", "inverse",
				"inversesqrt", "isinf", "isnan", "ldexp", "length", "lessThan",
				"lessThanEqual", "log", "log2", "matrixCompMult", "max",
				"memoryBarrier", "memoryBarrierAtomicCounter",
				"memoryBarrierBuffer", "memoryBarrierImage",
				"memoryBarrierShared", "min", "mix", "mod", "modf", "noise",
				"noise1", "noise2", "noise3", "noise4", "normalize", "not",
				"notEqual", "outerProduct", "packDouble2x32", "packHalf2x16",
				"packSnorm2x16", "packSnorm4x8", "packUnorm", "packUnorm2x16",
				"packUnorm4x8", "pow", "radians", "reflect", "refract", "round",
				"roundEven", "sign", "sin", "sinh", "smoothstep", "sqrt", "step",
				"tan", "tanh", "texelFetch", "texelFetchOffset", "texture",
				"textureGather", "textureGatherOffset", "textureGatherOffsets",
				"textureGrad", "textureGradOffset", "textureLod",
				"textureLodOffset", "textureOffset", "textureProj",
				"textureProjGrad", "textureProjGradOffset", "textureProjLod",
				"textureProjLodOffset", "textureProjOffset",
				"textureQueryLevels", "textureQueryLod", "textureSamples",
				"textureSize", "transpose", "trunc", "uaddCarry",
				"uintBitsToFloat", "umulExtended", "unpackDouble2x32",
				"unpackHalf2x16", "unpackSnorm2x16", "unpackSnorm4x8",
				"unpackUnorm", "unpackUnorm2x16", "unpackUnorm4x8",
				"usubBorrow");
	}

	private GlslWords() {
	}

	/** Style of the word from start to end, or IDENTIFIER for no word. */
	public static Style style(CharSequence text, int start, int end) {
		int i = lowerBound(WORDS, count, text, start, end);
		return i < count && compare(WORDS[i], text, start, end) == 0
				? STYLES[i]
				: Style.IDENTIFIER;
	}

	public static boolean isDirective(CharSequence text, int start, int end) {
		int i = lowerBound(DIRECTIVES, DIRECTIVES.length, text, start, end);
		return i < DIRECTIVES.length &&
				compare(DIRECTIVES[i], text, start, end) == 0;
	}

	public static List<String> matching(String prefix) {
		return matching(WORDS, count, prefix);
	}

	public static List<String> matchingDirectives(String prefix) {
		return matching(DIRECTIVES, DIRECTIVES.length, prefix);
	}

	private static List<String> matching(String[] words, int size,
			String prefix) {
		List<String> result = new ArrayList<>();
		if (prefix.isEmpty()) {
			return result;
		}
		for (int i = lowerBound(words, size, prefix, 0, prefix.length());
				i < size && words[i].startsWith(prefix); ++i) {
			result.add(words[i]);
		}
		return result;
	}

	// Later words replace earlier ones, which keeps WORDS sorted and unique.
	private static void add(Style style, String... words) {
		for (String word : words) {
			int i = Arrays.binarySearch(WORDS, 0, count, word);
			if (i < 0) {
				i = -i - 1;
				System.arraycopy(WORDS, i, WORDS, i + 1, count - i);
				System.arraycopy(STYLES, i, STYLES, i + 1, count - i);
				WORDS[i] = word;
				++count;
			}
			STYLES[i] = style;
		}
	}

	private static int lowerBound(String[] words, int size, CharSequence text,
			int start, int end) {
		int low = 0;
		int high = size;
		while (low < high) {
			int mid = (low + high) >>> 1;
			if (compare(words[mid], text, start, end) < 0) {
				low = mid + 1;
			} else {
				high = mid;
			}
		}
		return low;
	}

	private static int compare(String word, CharSequence text, int start,
			int end) {
		int length = Math.min(word.length(), end - start);
		for (int i = 0; i < length; ++i) {
			int d = word.charAt(i) - text.charAt(start + i);
			if (d != 0) {
				return d;
			}
		}
		return word.length() - (end - start);
	}
}
