#ifdef GL_ES
precision highp float;
#endif

uniform sampler2D sceneTex; // 0
uniform vec2 center; // Mouse position
uniform float time; // effect elapsed time
uniform float cameraZoom; // effect elapsed time
uniform vec2 resolution; // viewport resolution (in pixels)
uniform mat4 u_projTrans;

//uniform vec3 shockParams; // 10.0, 0.8, 0.1

varying vec2 v_texCoords;

const vec3 shockParams = vec3(10.0, 0.1, 0.1);
const float shockSizeScale = 20.0;   // bigger = smaller shock size

void main()
{
	// get pixel coordinates
	vec2 l_texCoords = v_texCoords;
//		vec2 center = vec2(0.5, 0.5);
	vec3 shockParams = vec3(10.0, 0.8, 0.1);

	float offset = (time- floor(time))/time;
	float CurrentTime = (time)*(offset);

	float width = resolution.x;
	float height = resolution.y;
	float aspect = height / width;

	vec2 aspectCorrectedCoords = vec2(l_texCoords.x, l_texCoords.y * aspect);
	vec2 aspectCorrectedCenter = vec2(center.x, center.y * aspect);

	//get distance from center
	float distance = shockSizeScale * cameraZoom * distance(aspectCorrectedCoords, aspectCorrectedCenter);

	if ((distance <= (CurrentTime + shockParams.z)) && (distance >= (CurrentTime - shockParams.z))) {
		float diff = (distance - CurrentTime);

		float powDiff = 0.0;
		if(distance > 0.05){
			powDiff = 1.0 - pow(abs(diff*shockParams.x), shockParams.y);
		}
		float diffTime = diff  * powDiff;
		vec2 diffUV = normalize(v_texCoords-center);
		//Perform the distortion and reduce the effect over time
		l_texCoords = v_texCoords + ((diffUV * diffTime)/(CurrentTime * distance * 100.0));
		//		l_texCoords.x *= aspect;
		vec4 color = gl_FragColor = texture2D(sceneTex, l_texCoords);
		color += (gl_FragColor * powDiff) / (time * distance * 100.0);
		gl_FragColor = color;
	} else {
		gl_FragColor = texture2D(sceneTex, l_texCoords);
	}
}