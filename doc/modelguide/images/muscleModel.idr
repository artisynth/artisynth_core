%!PS-Adobe-2.0 EPSF-1.2
%%Creator: idraw
%%DocumentFonts: Symbol Helvetica-Oblique
%%Pages: 1
%%BoundingBox: 45 278 456 543
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

/IdrawDict 52 dict def
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
/Symbol dup findfont def
/Helvetica-Oblique reencodeISO def

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
[ 0.747198 0 0 0.747198 0 0 ] concat
/originalCTM matrix currentmatrix def

Begin %I Pict
%I b 65535
2 0 0 [] 0 SetB
%I cfg u
%I cbg u
%I f u
%I p u
%I t u

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 14 -33 ] concat
%I
98 473 82 457 Line
%I 1
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
[ 1 -0 -0 1 14 -33 ] concat
%I
98 457 82 441 Line
%I 1
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
[ 1 -0 -0 1 14 -33 ] concat
%I
98 441 82 425 Line
%I 1
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
[ 1 -0 -0 1 14 -33 ] concat
%I
98 425 82 409 Line
%I 1
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
[ 1 -0 -0 1 14 -33 ] concat
%I
98 473 98 425 Line
%I 1
End

End %I eop

Begin %I Pict
%I b 65535
2 0 0 [] 0 SetB
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 8 -8 ] concat

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 14 -33 ] concat
%I
202 505 226 529 Line
%I 1
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
[ 1 0 0 1 14 -33 ] concat
%I
226 529 218 545 Line
%I 1
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
[ 1 0 0 1 14 -33 ] concat
%I
218 545 242 529 Line
%I 1
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
[ 1 0 0 1 14 -33 ] concat
%I
242 529 226 553 Line
%I 1
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
[ 1 0 0 1 14 -33 ] concat
%I
226 553 250 537 Line
%I 1
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
[ 1 0 0 1 14 -33 ] concat
%I
250 537 234 561 Line
%I 1
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
[ 1 0 0 1 14 -33 ] concat
%I
234 561 258 545 Line
%I 1
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
[ 0.5 0 0 0.5 113.5 194 ] concat
%I
317 636 285 684 Line
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
[ 0.5 0 0 0.5 113.5 194 ] concat
%I
285 684 333 652 Line
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
[ 0.5 0 0 0.5 113.5 194 ] concat
%I
333 652 301 700 Line
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
[ 0.5 0 0 0.5 113.5 194 ] concat
%I
301 700 333 684 Line
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
[ 0.5 0 0 0.5 113.5 194 ] concat
%I
333 684 381 732 Line
%I 2
End

End %I eop

Begin %I Pict
%I b 65535
2 0 0 [] 0 SetB
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 -8 8 ] concat

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 0 0 0.5 113.5 194 ] concat
%I
141 716 221 796 Line
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
[ 0.5 0 0 0.5 113.5 194 ] concat
%I
221 796 253 764 Line
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
[ 0.5 0 0 0.5 15.5 194 ] concat
%I
417 796 369 684 Line
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
[ 1 0 0 1 14 -33 ] concat
%I
154 553 178 577 Line
%I 1
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
[ 1 0 0 1 14 -33 ] concat
%I
170 585 186 569 Line
%I 1
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
[ 1 0 0 1 14 -33 ] concat
%I
186 569 226 609 Line
%I 1
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
[ 0.5 0 0 0.5 113.5 194 ] concat
%I
237 780 285 828 Line
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
[ 0.5 0 0 0.5 15.5 194 ] concat
%I
337 716 449 764 Line
%I 2
End

End %I eop

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 15.5 194 ] concat
%I
289 668 417 540 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 15.5 194 ] concat
%I
465 844 593 716 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -481 26 ] concat
%I
761 559 841 639 Line
%I 1
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 -481 26 ] concat
%I
841 639 913 639 Line
%I 1
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
880 551 896 583 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
896 583 912 519 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
912 519 928 583 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
928 583 944 519 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
944 519 960 583 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
960 583 976 519 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
976 519 992 583 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
992 583 1008 519 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
1008 519 1024 583 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
1024 583 1040 551 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -8 388.5 ] concat
%I
1040 551 1200 551 Line
%I 2
End

Begin %I Pict
%I b 65535
2 0 0 [] 0 SetB
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ -1 0 -0 1 704 248 ] concat

Begin %I Line
%I b 65535
1 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 1 -0 -0 1 14 -33 ] concat
%I
98 473 82 457 Line
%I 1
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
[ 1 -0 -0 1 14 -33 ] concat
%I
98 457 82 441 Line
%I 1
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
[ 1 -0 -0 1 14 -33 ] concat
%I
98 441 82 425 Line
%I 1
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
[ 1 -0 -0 1 14 -33 ] concat
%I
98 425 82 409 Line
%I 1
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
[ 1 -0 -0 1 14 -33 ] concat
%I
98 473 98 425 Line
%I 1
End

End %I eop

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 73.5 308.5 ] concat
%I
77 215 237 375 Line
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
[ 0.5 -0 -0 0.5 73.5 308.5 ] concat
%I
77 215 301 215 Line
%I 2
End

Begin %I BSpl
%I b 65535
1 1 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 70.5 309 ] concat
%I 5
187 311
209 296
228 269
234 245
236 221
5 BSpl
%I 2
End

Begin %I Line
%I b 65535
1 1 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 73.5 308.5 ] concat
%I
573 759 1037 759 Line
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
[ 0.5 -0 -0 0.5 81.5 308.5 ] concat
%I
61 743 61 775 Line
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
[ 0.5 -0 -0 0.5 73.5 308.5 ] concat
%I
573 743 573 775 Line
%I 2
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-symbol-medium-r-normal--24-*
Symbol 24 SetF
%I t
[ 1 0 0 1 159.5 448 ] concat
%I
[
(f)
] Text
End

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 215 -274 ] concat

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 1 0 0 1 245 900 ] concat
%I
[
(t)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 1 0 0 1 265 899 ] concat
%I
[
(t)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--18-*
Helvetica-Oblique 18 SetF
%I t
[ 1 0 0 1 237 908 ] concat
%I
[
(f  \(l  \))
] Text
End

End %I eop

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 0.696786 0.717279 -0.717279 0.696786 603.507 -411.533 ] concat

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-symbol-medium-r-normal--18-*
Symbol 18 SetF
%I t
[ 1 0 0 1 447 933.5 ] concat
%I
[
(b)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--18-*
Helvetica-Oblique 18 SetF
%I t
[ 1 0 0 1 458 932 ] concat
%I
[
(v)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 1 0 0 1 467.5 923.5 ] concat
%I
[
(f)
] Text
End

End %I eop

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 0.662453 0.749103 -0.749103 0.662453 528.093 -206.646 ] concat

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--18-*
Helvetica-Oblique 18 SetF
%I t
[ 1 0 0 1 314.5 796 ] concat
%I
[
(a f  \(l  \) f  \(v  \))
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 1 0 0 1 336 787 ] concat
%I
[
(L)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 1 0 0 1 378 787.5 ] concat
%I
[
(V)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 1 0 0 1 407 787 ] concat
%I
[
(f)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 1 0 0 1 357 788 ] concat
%I
[
(f)
] Text
End

End %I eop

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 0.707107 0.707107 -0.707107 0.707107 500.804 -435.239 ] concat

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--18-*
Helvetica-Oblique 18 SetF
%I t
[ 1 0 0 1 479.5 806.5 ] concat
%I
[
(f   \(l  \))
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 1 0 0 1 483.5 798.5 ] concat
%I
[
(P)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 1 0 0 1 512 799.5 ] concat
%I
[
(f)
] Text
End

End %I eop

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 16.5 349 ] concat
%I
351 294 383 326 Line
%I 2
End

Begin %I Pict
%I b u
%I cfg u
%I cbg u
%I f u
%I p u
%I t
[ 1 0 0 1 -8 -8 ] concat

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 0 0 0.5 113.5 194 ] concat
%I
189 668 221 636 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 0 0 0.5 113.5 194 ] concat
%I
221 700 253 668 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 16.5 349 ] concat
%I
383 358 447 422 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 16.5 349 ] concat
%I
415 326 479 390 Line
%I 2
End

End %I eop

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 16.5 349 ] concat
%I
415 358 463 406 Line
%I 2
End

Begin %I Line
%I b 65535
2 0 0 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 16.5 349 ] concat
%I
527 470 511 454 Line
%I 2
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 0.668965 0.743294 -0.743294 0.668965 159.277 590.615 ] concat
%I
[
(^)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 0.668965 0.743294 -0.743294 0.668965 192.777 624.115 ] concat
%I
[
(^)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 0.668965 0.743294 -0.743294 0.668965 252.277 572.615 ] concat
%I
[
(^)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--14-*
Helvetica-Oblique 14 SetF
%I t
[ 0.668965 0.743294 -0.743294 0.668965 284.777 500.115 ] concat
%I
[
(^)
] Text
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
[ 0.5 -0 -0 0.5 -82 291 ] concat
%I
324 746 356 746 Line
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
[ 0.5 -0 -0 0.5 -90 291 ] concat
%I
372 250 340 250 Line
%I 2
End

Begin %I Line
%I b 65535
1 1 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -90 291 ] concat
%I
404 794 900 794 Line
%I 2
End

Begin %I Line
%I b 65535
1 1 1 [] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -90 291 ] concat
%I
356 746 356 250 Line
%I 2
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--24-*
Helvetica-Oblique 24 SetF
%I t
[ 1 0 0 1 64 551 ] concat
%I
[
(H)
] Text
End

Begin %I Line
%I b 255
1 0 0 [8 8] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 -6.5 291 ] concat
%I
237 746 733 746 Line
%I 2
End

Begin %I Line
%I b 255
1 0 0 [8 8] 0 SetB
%I cfg Black
0 0 0 SetCFg
%I cbg White
1 1 1 SetCBg
none SetP %I p n
%I t
[ 0.5 -0 -0 0.5 84.5 353.5 ] concat
%I
55 621 55 173 Line
%I 2
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--24-*
Helvetica-Oblique 24 SetF
%I t
[ 1 0 0 1 228.5 720.5 ] concat
%I
[
( l)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--18-*
Helvetica-Oblique 18 SetF
%I t
[ 1 0 0 1 240.5 711.5 ] concat
%I
[
(m)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--24-*
Helvetica-Oblique 24 SetF
%I t
[ 1 0 0 1 463.5 717.5 ] concat
%I
[
( l)
] Text
End

Begin %I Text
%I cfg Black
0 0 0 SetCFg
%I f -*-helvetica-medium-o-normal--18-*
Helvetica-Oblique 18 SetF
%I t
[ 1 0 0 1 476.5 709.5 ] concat
%I
[
(t)
] Text
End

End %I eop

showpage

%%Trailer

end
