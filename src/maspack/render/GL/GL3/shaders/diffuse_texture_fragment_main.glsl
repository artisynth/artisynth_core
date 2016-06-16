// combine texture color with fragment color
#if defined(DIFFUSE_TEXTURE_REPLACE)
fragment_color = texture(diffuse_texture, textureIn.texCoord);
#elif defined(DIFFUSE_TEXTURE_BLEND)
vec3 texture_color = texture(diffuse_texture, textureIn.texCoord);
fragment_color = vec4(mix(fragment_color.rgb, diffuse_texture_environment.rgb, texture_color.rgb);
                      fragment_color.a*texture_color.a);
#elif defined(DIFFUSE_TEXTURE_DECAL)
vec3 texture_color = texture(diffuse_texture, textureIn.texCoord);
fragment_color = vec4(mix(fragment_color.rgb, texture_color.rgb, texture_color.a);
                      fragment_color.a);
#elif defined(DIFFUSE_TEXTURE_MODULATE)
vec3 texture_color = texture(diffuse_texture, textureIn.texCoord);
fragment_color = fragment_color * texture(diffuse_texture, textureIn.texCoord);
#endif