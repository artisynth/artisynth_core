%!PS-Adobe-2.0 EPSF-1.2
%%Creator: idraw
%%DocumentFonts: Helvetica
%%Pages: 1
%%BoundingBox: 21 320 330 606
%%EndComments

%%BeginIdrawPrologue
/arrowhead {
0 begin
transform originalCTM itransform
/taily exch def
/tailx exch def
transform originalCTM itransform
/tipy exch def
/tipx exch def
/dy tipy taily sub def
/dx tipx tailx sub def
/angle dx 0 ne dy 0 ne or { dy dx atan } { 90 } ifelse def
gsave
originalCTM setmatrix
tipx tipy translate
angle rotate
newpath
arrowHeight neg arrowWidth 2 div moveto
0 0 lineto
arrowHeight neg arrowWidth 2 div neg lineto
patternNone not {
originalCTM setmatrix
/padtip arrowHeight 2 exp 0.25 arrowWidth 2 exp mul add sqrt brushWidth mul
arrowWidth div def
/padtail brushWidth 2 div def
tipx tipy translate
angle rotate
padtip 0 translate
arrowHeight padtip add padtail add arrowHeight div dup scale
arrowheadpath
ifill
} if
brushNone not {
originalCTM setmatrix
tipx tipy translate
angle rotate
arrowheadpath
istroke
} if
grestore
end
} dup 0 9 dict put def

/arrowheadpath {
newpath
arrowHeight neg arrowWidth 2 div moveto
0 0 lineto
arrowHeight neg arrowWidth 2 div neg lineto
} def

/leftarrow {
0 begin
y exch get /taily exch def
x exch get /tailx exch def
y exch get /tipy exch def
x exch get /tipx exch def
brushLeftArrow { tipx tipy tailx taily arrowhead } if
end
} dup 0 4 dict put def

/rightarrow {
0 begin
y exch get /tipy exch def
x exch get /tipx exch def
y exch get /taily exch def
x exch get /tailx exch def
brushRightArrow { tipx tipy tailx taily arrowhead } if
end
} dup 0 4 dict put def

%%EndIdrawPrologue

/arrowHeight 11 def
/arrowWidth 5 def

/IdrawDict 51 dict def
IdrawDict begin

/reencodeISO {
dup dup findfont dup length dict begin
{ 1 index /FID ne { def }{ pop pop } ifelse } forall
/Encoding ISOLatin1Encoding def
currentdict end definefont
} def

/ISOLatin1Encoding [
/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef
/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef
/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef
/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef
/space/exclam/quotedbl/numbersign/dollar/percent/ampersand/quoteright
/parenleft/parenright/asterisk/plus/comma/minus/period/slash
/zero/one/two/three/four/five/six/seven/eight/nine/colon/semicolon
/less/equal/greater/question/at/A/B/C/D/E/F/G/H/I/J/K/L/M/N
/O/P/Q/R/S/T/U/V/W/X/Y/Z/bracketleft/backslash/bracketright
/asciicircum/underscore/quoteleft/a/b/c/d/e/f/g/h/i/j/k/l/m
/n/o/p/q/r/s/t/u/v/w/x/y/z/braceleft/bar/braceright/asciitilde
/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef
/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef/.notdef
/.notdef/dotlessi/grave/acute/circumflex/tilde/macron/breve
/dotaccent/dieresis/.notdef/ring/cedilla/.notdef/hungarumlaut
/ogonek/caron/space/exclamdown/cent/sterling/currency/yen/brokenbar
/section/dieresis/copyright/ordfeminine/guillemotleft/logicalnot
/hyphen/registered/macron/degree/plusminus/twosuperior/threesuperior
/acute/mu/paragraph/periodcentered/cedilla/onesuperior/ordmasculine
/guillemotright/onequarter/onehalf/threequarters/questiondown
/Agrave/Aacute/Acircumflex/Atilde/Adieresis/Aring/AE/Ccedilla
/Egrave/Eacute/Ecircumflex/Edieresis/Igrave/Iacute/Icircumflex
/Idieresis/Eth/Ntilde/Ograve/Oacute/Ocircumflex/Otilde/Odieresis
/multiply/Oslash/Ugrave/Uacute/Ucircumflex/Udieresis/Yacute
/Thorn/germandbls/agrave/aacute/acircumflex/atilde/adieresis
/aring/ae/ccedilla/egrave/eacute/ecircumflex/edieresis/igrave
/iacute/icircumflex/idieresis/eth/ntilde/ograve/oacute/ocircumflex
/otilde/odieresis/divide/oslash/ugrave/uacute/ucircumflex/udieresis
/yacute/thorn/ydieresis
] def
/Helvetica reencodeISO def

/none null def
/numGraphicParameters 17 def
/stringLimit 65535 def

/Begin {
save
numGraphicParameters dict begin
} def

/End {
end
restore
} def

/SetB {
dup type /nulltype eq {
pop
false /brushRightArrow idef
false /brushLeftArrow idef
true /brushNone idef
} {
/brushDashOffset idef
/brushDashArray idef
0 ne /brushRightArrow idef
0 ne /brushLeftArrow idef
/brushWidth idef
false /brushNone idef
} ifelse
} def

/SetCFg {
/fgblue idef
/fggreen idef
/fgred idef
} def

/SetCBg {
/bgblue idef
/bggreen idef
/bgred idef
} def

/SetF {
/printSize idef
/printFont idef
} def

/SetP {
dup type /nulltype eq {
pop true /patternNone idef
} {
dup -1 eq {
/patternGrayLevel idef
/patternString idef
} {
/patternGrayLevel idef
} ifelse
false /patternNone idef
} ifelse
} def

/BSpl {
0 begin
storexyn
newpath
n 1 gt {
0 0 0 0 0 0 1 1 true subspline
n 2 gt {
0 0 0 0 1 1 2 2 false subspline
1 1 n 3 sub {
/i exch def
i 1 sub dup i dup i 1 add dup i 2 add dup false subspline
} for
n 3 sub dup n 2 sub dup n 1 sub dup 2 copy false subspline
} if
n 2 sub dup n 1 sub dup 2 copy 2 copy false subspline
patternNone not brushLeftArrow not brushRightArrow not and and { ifill } if
brushNone not { istroke } if
0 0 1 1 leftarrow
n 2 sub dup n 1 sub dup rightarrow
} if
end
} dup 0 4 dict put def

/Circ {
newpath
0 360 arc
closepath
patternNone not { ifill } if
brushNone not { istroke } if
} def

/CBSpl {
0 begin
dup 2 gt {
storexyn
newpath
n 1 sub dup 0 0 1 1 2 2 true subspline
1 1 n 3 sub {
/i exch def
i 1 sub dup i dup i 1 add dup i 2 add dup false subspline
} for
n 3 sub dup n 2 sub dup n 1 sub dup 0 0 false subspline
n 2 sub dup n 1 sub dup 0 0 1 1 false subspline
patternNone not { ifill } if
brushNone not { istroke } if
} {
Poly
} ifelse
end
} dup 0 4 dict put def

/Elli {
0 begin
newpath
4 2 roll
translate
scale
0 0 1 0 360 arc
closepath
patternNone not { ifill } if
brushNone not { istroke } if
end
} dup 0 1 dict put def

/Line {
0 begin
2 storexyn
newpath
x 0 get y 0 get moveto
x 1 get y 1 get lineto
brushNone not { istroke } if
0 0 1 1 leftarrow
0 0 1 1 rightarrow
end
} dup 0 4 dict put def

/MLine {
0 begin
storexyn
newpath
n 1 gt {
x 0 get y 0 get moveto
1 1 n 1 sub {
/i exch def
x i get y i get lineto
} for
patternNone not brushLeftArrow not brushRightArrow not and and { ifill } if
brushNone not { istroke } if
0 0 1 1 leftarrow
n 2 sub dup n 1 sub dup rightarrow
} if
end
} dup 0 4 dict put def

/Poly {
3 1 roll
newpath
moveto
-1 add
{ lineto } repeat
closepath
patternNone not { ifill } if
brushNone not { istroke } if
} def

/Rect {
0 begin
/t exch def
/r exch def
/b exch def
/l exch def
newpath
l b moveto
l t lineto
r t lineto
r b lineto
closepath
patternNone not { ifill } if
brushNone not { istroke } if
end
} dup 0 4 dict put def

/Text {
ishow
} def

/idef {
dup where { pop pop pop } { exch def } ifelse
} def

/ifill {
0 begin
gsave
patternGrayLevel -1 ne {
fgred bgred fgred sub patternGrayLevel mul add
fggreen bggreen fggreen sub patternGrayLevel mul add
fgblue bgblue fgblue sub patternGrayLevel mul add setrgbcolor
eofill
} {
eoclip
originalCTM setmatrix
pathbbox /t exch def /r exch def /b exch def /l exch def
/w r l sub ceiling cvi def
/h t b sub ceiling cvi def
/imageByteWidth w 8 div ceiling cvi def
/imageHeight h def
bgred bggreen bgblue setrgbcolor
eofill
fgred fggreen fgblue setrgbcolor
w 0 gt h 0 gt and {
l w add b translate w neg h scale
w h true [w 0 0 h neg 0 h] { patternproc } imagemask
} if
} ifelse
grestore
end
} dup 0 8 dict put def

/istroke {
gsave
brushDashOffset -1 eq {
[] 0 setdash
1 setgray
} {
brushDashArray brushDashOffset setdash
fgred fggreen fgblue setrgbcolor
} ifelse
brushWidth setlinewidth
originalCTM setmatrix
stroke
grestore
} def

/ishow {
0 begin
gsave
fgred fggreen fgblue setrgbcolor
/fontDict printFont printSize scalefont dup setfont def
/descender fontDict begin 0 /FontBBox load 1 get FontMatrix end
transform exch pop def
/vertoffset 1 printSize sub descender sub def {
0 vertoffset moveto show
/vertoffset vertoffset printSize sub def
} forall
grestore
end
} dup 0 3 dict put def
/patternproc {
0 begin
/patternByteLength patternString length def
/patternHeight patternByteLength 8 mul sqrt cvi def
/patternWidth patternHeight def
/patternByteWidth patternWidth 8 idiv def
/imageByteMaxLength imageByteWidth imageHeight mul
stringLimit patternByteWidth sub min def
/imageMaxHeight imageByteMaxLength imageByteWidth idiv patternHeight idiv
patternHeight mul patternHeight max def
/imageHeight imageHeight imageMaxHeight sub store
/imageString imageByteWidth imageMaxHeight mul patternByteWidth add string def
0 1 imageMaxHeight 1 sub {
/y exch def
/patternRow y patternByteWidth mul patternByteLength mod def
/patternRowString patternString patternRow patternByteWidth getinterval def
/imageRow y imageByteWidth mul def
0 patternByteWidth imageByteWidth 1 sub {
/x exch def
imageString imageRow x add patternRowString putinterval
} for
} for
imageString
end
} dup 0 12 dict put def

/min {
dup 3 2 roll dup 4 3 roll lt { exch } if pop
} def

/max {
dup 3 2 roll dup 4 3 roll gt { exch } if pop
} def

/midpoint {
0 begin
/y1 exch def
/x1 exch def
/y0 exch def
/x0 exch def
x0 x1 add 2 div
y0 y1 add 2 div
end
} dup 0 4 dict put def

/thirdpoint {
0 begin
/y1 exch def
/x1 exch def
/y0 exch def
/x0 exch def
x0 2 mul x1 add 3 div
y0 2 mul y1 add 3 div
end
} dup 0 4 dict put def

/subspline {
0 begin
/movetoNeeded exch def
y exch get /y3 exch def
x exch get /x3 exch def
y exch get /y2 exch def
x exch get /x2 exch def
y exch get /y1 exch def
x exch get /x1 exch def
y exch get /y0 exch def
x exch get /x0 exch def
x1 y1 x2 y2 thirdpoint
/p1y exch def
/p1x exch def
x2 y2 x1 y1 thirdpoint
/p2y exch def
/p2x exch def
x1 y1 x0 y0 thirdpoint
p1x p1y midpoint
/p0y exch def
/p0x exch def
x2 y2 x3 y3 thirdpoint
p2x p2y midpoint
/p3y exch def
/p3x exch def
movetoNeeded { p0x p0y moveto } if
p1x p1y p2x p2y p3x p3y curveto
end
} dup 0 17 dict put def

/storexyn {
/n exch def
/y n array def
/x n array def
n 1 sub -1 0 {
/i exch def
y i 3 2 roll put
x i 3 2 roll put
} for
} def

/SSten {
fgred fggreen fgblue setrgbcolor
dup true exch 1 0 0 -1 0 6 -1 roll matrix astore
} def

/FSten {
dup 3 -1 roll dup 4 1 roll exch
newpath
0 0 moveto
dup 0 exch lineto
exch dup 3 1 roll exch lineto
0 lineto
closepath
bgred bggreen bgblue setrgbcolor
eofill
SSten
} def

/Rast {
exch dup 3 1 roll 1 0 0 -1 0 6 -1 roll matrix astore
} def

%%EndProlog

%I Idraw 13 Grid 8 8 

%%Page: 1 1

Begin
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 0.74759 0 0 0.74759 0 0 ] concat
/originalCTM matrix currentmatrix def

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -17 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -1 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 31 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 15 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 47 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 63 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 79 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 111 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 127 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 143 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 159 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 191 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 175 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 207 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 223 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 239 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 271 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 255 57 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -17 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -1 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 31 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 15 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 47 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 63 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 79 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 111 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 127 17 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 -23 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -17 -23 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -1 -23 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 31 -23 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 15 -23 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 47 -23 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 63 -23 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 79 -23 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -17 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -1 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 31 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 15 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 47 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 63 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 79 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 111 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 127 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 143 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 159 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 175 -63 ] concat
%I
153 727 169 751 Rect
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-r-normal--18-*
Helvetica 18 SetF
%I t
[ 1 0 0 1 32 801 ] concat
%I
[
(positions)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-r-normal--18-*
Helvetica 18 SetF
%I t
[ 1 0 0 1 42 761.5 ] concat
%I
[
(normals)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-r-normal--18-*
Helvetica 18 SetF
%I t
[ 1 0 0 1 56 721 ] concat
%I
[
(colors)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-r-normal--18-*
Helvetica 18 SetF
%I t
[ 1 0 0 1 48 692 ] concat
%I
[
(texture )
(coords)
] Text
End

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 -64 32 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 111 57 ] concat
%I
185 527 201 543 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 111 41 ] concat
%I
185 527 201 543 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 111 25 ] concat
%I
185 527 201 543 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 111 9 ] concat
%I
185 527 201 543 Rect
End

End %I eop

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
201 479 217 543 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -65 89 ] concat
%I
201 479 217 543 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -81 89 ] concat
%I
201 479 217 543 Rect
End

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 -40 32 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 39 57 ] concat
%I
201 479 217 543 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 527 289 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 511 289 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 495 289 495 Line
%I 1
End

End %I eop

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
153 527 169 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
153 511 169 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
153 495 169 495 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
169 495 185 495 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
169 511 185 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
169 527 185 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
201 527 217 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
209 511 217 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
209 511 201 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 89 ] concat
%I
201 495 217 495 Line
%I 1
End

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 -24 32 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 39 57 ] concat
%I
201 479 217 543 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 527 289 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 511 289 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 495 289 495 Line
%I 1
End

End %I eop

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 -56 32 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 39 57 ] concat
%I
201 479 217 543 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 527 289 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 511 289 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 495 289 495 Line
%I 1
End

End %I eop

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 -88 32 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 39 57 ] concat
%I
201 479 217 543 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 527 289 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 511 289 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 495 289 495 Line
%I 1
End

End %I eop

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
201 479 217 543 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 63 89 ] concat
%I
201 479 217 543 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 47 89 ] concat
%I
201 479 217 543 Rect
End

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 88 32 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 39 57 ] concat
%I
201 479 217 543 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 527 289 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 511 289 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 495 289 495 Line
%I 1
End

End %I eop

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
153 527 169 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
153 511 169 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
153 495 169 495 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
169 495 185 495 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
169 511 185 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
169 527 185 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
201 527 217 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
209 511 217 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
209 511 201 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 95 89 ] concat
%I
201 495 217 495 Line
%I 1
End

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 104 32 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 39 57 ] concat
%I
201 479 217 543 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 527 289 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 511 289 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 495 289 495 Line
%I 1
End

End %I eop

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 72 32 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 39 57 ] concat
%I
201 479 217 543 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 527 289 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 511 289 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 495 289 495 Line
%I 1
End

End %I eop

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 40 32 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 39 57 ] concat
%I
201 479 217 543 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 527 289 527 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 511 289 511 Line
%I 1
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 57 ] concat
%I
273 495 289 495 Line
%I 1
End

End %I eop

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-r-normal--18-*
Helvetica 18 SetF
%I t
[ 1 0 0 1 40 609 ] concat
%I
[
(vertices)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-r-normal--14-*
Helvetica 14 SetF
%I t
[ 1 0 0 1 235 581 ] concat
%I
[
(-1)
] Text
End

Begin %I Elli
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 143 369.5 ] concat
%I
193 446 3 3 Elli
End

Begin %I Elli
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 144 401 ] concat
%I
193 446 3 3 Elli
End

Begin %I Elli
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 143.5 385 ] concat
%I
193 446 3 3 Elli
End

Begin %I BSpl
%I b 65535
1 0 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
%I p
0 SetP
%I t
[ 0.25 -0 -0 0.25 134.25 529 ] concat
%I 6
423 380
487 380
679 412
807 796
673 1058
553 1064
6 BSpl
%I 4
End

Begin %I BSpl
%I b 65535
1 0 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
%I p
0 SetP
%I t
[ 0.25 -0 -0 0.25 134.25 529 ] concat
%I 5
423 316
647 348
647 732
519 860
361 904
5 BSpl
%I 4
End

Begin %I BSpl
%I b 65535
1 0 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
%I p
0 SetP
%I t
[ 0.25 -0 -0 0.25 134.25 529 ] concat
%I 6
423 252
487 252
743 412
551 636
327 700
169 744
6 BSpl
%I 4
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -33 -215 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -17 -215 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -1 -215 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 31 -215 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 15 -215 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 47 -215 ] concat
%I
153 727 169 751 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
193 122 449 170 Rect
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
193 42 289 90 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
257 170 257 122 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
321 170 321 122 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
385 170 385 122 Line
%I 2
End

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 71.5 411 ] concat
%I
193 42 289 90 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
225 170 225 122 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
289 170 289 122 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
353 170 353 122 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
417 170 417 122 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
225 74 225 90 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
225 90 225 42 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
257 90 257 42 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
321 90 321 42 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
353 90 353 42 Line
%I 2
End

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 40 -8 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 79.5 419 ] concat
%I
193 42 289 90 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 31.5 419 ] concat
%I
321 90 321 42 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 31.5 419 ] concat
%I
353 90 353 42 Line
%I 2
End

End %I eop

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 88 -8 ] concat

Begin %I Rect
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 79.5 419 ] concat
%I
193 42 289 90 Rect
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 31.5 419 ] concat
%I
321 90 321 42 Line
%I 2
End

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg LtGray
0.762951 0.762951 0.762951 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 31.5 419 ] concat
%I
353 90 353 42 Line
%I 2
End

End %I eop

Begin %I Rect
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
417 314 449 442 Rect
End

Begin %I Rect
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
321 202 353 250 Rect
End

Begin %I Rect
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
385 122 449 170 Rect
End

Begin %I Rect
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 23.5 411 ] concat
%I
481 42 577 90 Rect
End

Begin %I BSpl
%I b 65535
1 0 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
%I p
0 SetP
%I t
[ 0.25 -0 -0 0.25 108.75 422.5 ] concat
%I 4
335 400
219 456
201 522
205 577
4 BSpl
%I 4
End

Begin %I BSpl
%I b 65535
1 0 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
%I p
0 SetP
%I t
[ 0.25 -0 -0 0.25 108.75 422.5 ] concat
%I 5
779 84
883 203
928 347
906 518
845 579
5 BSpl
%I 4
End

Begin %I BSpl
%I b 65535
1 0 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
%I p
0 SetP
%I t
[ 0.25 -0 -0 0.25 108.75 422.5 ] concat
%I 4
718 86
820 257
834 473
782 579
4 BSpl
%I 4
End

Begin %I BSpl
%I b 65535
1 0 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
%I p
0 SetP
%I t
[ 0.25 -0 -0 0.25 108.75 422.5 ] concat
%I 4
523 246
667 328
752 447
783 577
4 BSpl
%I 4
End

Begin %I BSpl
%I b 65535
1 0 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
%I p
0 SetP
%I t
[ 0.25 -0 -0 0.25 108.75 422.5 ] concat
%I 4
653 87
752 261
720 499
658 579
4 BSpl
%I 4
End

Begin %I BSpl
%I b 65535
1 0 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
%I p
0 SetP
%I t
[ 0.25 -0 -0 0.25 108.75 422.5 ] concat
%I 4
463 244
577 338
663 434
658 575
4 BSpl
%I 4
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-r-normal--18-*
Helvetica 18 SetF
%I t
[ 1 0 0 1 54.5 530.5 ] concat
%I
[
(points)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-r-normal--18-*
Helvetica 18 SetF
%I t
[ 1 0 0 1 66.5 491 ] concat
%I
[
(lines)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-r-normal--18-*
Helvetica 18 SetF
%I t
[ 1 0 0 1 35.5 452.5 ] concat
%I
[
(triangles)
] Text
End

End %I eop

showpage

%%Trailer

end
