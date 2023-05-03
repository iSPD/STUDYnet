package com.ispd.mommybook.ocr.detection;

import android.annotation.SuppressLint;

import com.ispd.mommybook.ocr.recognition.OCRRecognition;
import com.ispd.mommybook.utils.UtilsLogger;

import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRotatedRect;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class OCRDetectionDecodeResult {
    private static final UtilsLogger LOGGER = new UtilsLogger();

    public OCRDetectionDecodeResult() {

    }

    //    static public boolean drawTextLines(int in_outputSize, Mat in_scoresMat, Mat in_geometryMat)
    @SuppressLint("NewApi")
    public static Point[][] decodeDetectionResult(Mat in_scoresMat, Mat in_geometryMat,
                                                  int in_outputSize, int coverOrScoring)
    {
        float scoreThresh = 0.6f;//0.5f;
        float nmsThresh = 0.1f;//0.4f;

        Size siz = new Size(in_outputSize*4, in_outputSize*4);

        LOGGER.i("SallyDetect outputSize : "+siz);

        List<Float> confidencesList = new ArrayList<>();
        List<RotatedRect> boxesList = decode(in_scoresMat, in_geometryMat, confidencesList, scoreThresh);

        LOGGER.i("SallyDetect confidences : "+confidencesList.size());

        if( confidencesList.size() == 0 )
        {
            return null;
        }

        // Apply non-maximum suppression procedure.
        MatOfFloat confidences = new MatOfFloat(Converters.vector_float_to_Mat(confidencesList));
        RotatedRect[] boxesArray = boxesList.toArray(new RotatedRect[0]);
        MatOfRotatedRect boxes = new MatOfRotatedRect(boxesArray);
        MatOfInt indices = new MatOfInt();
        //Opencv에 Dnn 모듈 빌드 필요
//        Dnn.NMSBoxesRotated(boxes, confidences, scoreThresh, nmsThresh, indices);

        // Render detections
//        Point ratio = new Point(1080.f/siz.width, 1440.f/siz.height);
        Point ratio = new Point(1600.f/siz.width, 1200.f/siz.height);

//        아래 코드가 굳이 함수 안에 있을 필요는 없음. 고정값임.
        Point ratioRecog = new Point(1280.f/siz.width, 960.f/siz.height); //for recognition (sally)
        int[] indexes = indices.toArray();

        // detect된 bbox의 개수임.
        int numOfDetectedBBox = indexes.length;//for recognition (sally)
        LOGGER.d("SallyDetect num of BBOX = " + numOfDetectedBBox);

        // mRotRectList 는 정렬함수를 이용하기 위해 Comparable 클래스를 상속한 DetectedRects를 담은 리스트임.
        // mRotRectList 는 빈번하게 사용되므로 글로벌로 선언해서 사용함.
        List<DetectedRects> rotRectList = new ArrayList<>(); //for recognition (sally)
        rotRectList.clear();
        for(int i = 0; i < indexes.length; ++i) {
            RotatedRect rot = boxesArray[indexes[i]];
            rotRectList.add(new DetectedRects(indexes[i], rot));
        }

        float expandRatioX, expandRatioY;
        if(coverOrScoring == OCRDetection.DetectionType.COVER_DETECTION)
        {
            expandRatioX = 0.05f;
            expandRatioY = 0.0f; //0.02f
            // 표지인식인 경우 detect 된 bbox 개수가 max 값을 초과하는 경우, max값 만큼의 개수만 인식하기 위함.
            // 채점모드인 경우에는 max값을 두면 안되고 모든 bbox를 인식해야함.
            if(numOfDetectedBBox > OCRRecognition.RECOG_BOX_MAX) {
                numOfDetectedBBox = OCRRecognition.RECOG_BOX_MAX;//for recognition (sally)
            }
            //면적이 큰 순서대로 정렬
            rotRectList.sort(Comparator.reverseOrder()); //Comparator.naturalOrder()

        }else { //coverOrScoring == DetectionType.SCORING_DETECTION, 채점모드.
            expandRatioX = 0.0f;
            expandRatioY = 0.0f;
            //채점모드인 경우 bbox를 우->좌 && 상->하 순서대로 정렬하기.
            //TODO
        }

        Point recogBoxPoint[][] = createRecogPointArray(numOfDetectedBBox);

//      for(int i = 0; i < indexes.length; ++i) {
        for(int i = 0; i < numOfDetectedBBox; ++i) {
            //RotatedRect rot = boxesArray[indexes[i]];
            RotatedRect rot = rotRectList.get(i).getRotRect();  //for recognition (sally)
            Point[] vertices;
            Point[] verticesRecog = new Point[4]; //for recognition (sally)
//            rot.points(vertices);

            // bbox를 주어진 팩터만큼 확장시킴.
            vertices = expandedRotatedRect(rot, expandRatioX, expandRatioY);
//            rot.points(verticesRecog); //for recognition (sally)

            for (int j = 0; j < 4; ++j) {
                //1. 인식루틴에 들어갈 점 생성.
                verticesRecog[j] = new Point(vertices[j].x * ratioRecog.x,
                        vertices[j].y * ratioRecog.y); //for recognition (sally)
//                verticesRecog[j].x = vertices[j].x * ratioRecog.x; //for recognition (sally)
//                verticesRecog[j].y = vertices[j].y * ratioRecog.y; //for recognition (sally)

                //2. 점을 인식용 입력배열에 저장
                recogBoxPoint[i][j].x = verticesRecog[j].x;
                recogBoxPoint[i][j].y = verticesRecog[j].y;

                LOGGER.i("SallyDetect vertices[%d] : %f %f", j, vertices[j].x, vertices[j].y);

                vertices[j].x *= 2.f / 4.f;
                vertices[j].y *= 3.f / 5.f;

                vertices[j].x += 416.f * 1.f / 4.f;
                vertices[j].y += 416.f * 1.f / 5.f;

                //3. 화면에 box를 그리기 위한 점 생성.
                vertices[j].x *= ratio.x;
                vertices[j].y *= ratio.y;
            }
        }

        // TODO : 결과값 어떻게 나오는지 확인하기~~~


//        //인식 쓰레드 생성. 인식중이면 그냥 넘어감.
//        //TODO : WordRecognition handler에게 인식하라고 메시지 보내기
//        mWordRecognition.setInputMat(mSrcimgMat);
//        mWordRecognition.updatePoints(numOfDetectedBBox, recogBoxPoint);
////        mWordRecognition.startRecognition();
//        mWordRecognition.mWordRecogHandler.sendEmptyMessage(0);

        return recogBoxPoint;
    }

    public static Point[][] createRecogPointArray(final int in_numOfBoxes) { //for recognition (sally)
        Point recogBoxPoint[][] = new Point[in_numOfBoxes][];
        for (int i = 0; i < in_numOfBoxes; i++) {
            recogBoxPoint[i] = new Point[4];
            for (int j = 0; j < 4; j++) {
                recogBoxPoint[i][j] = new Point();
            }
        }
        return recogBoxPoint;
    }

    private static List<RotatedRect> decode(Mat scores, Mat geometry, List<Float> confidences, float scoreThresh) {
        int W = geometry.cols();
        int H = geometry.rows() / 5;

        List<RotatedRect> detections = new ArrayList<>();
        for (int y = 0; y < H; ++y) {
            Mat scoresData = scores.row(y);
            Mat x0Data = geometry.submat(0, H, 0, W).row(y);
            Mat x1Data = geometry.submat(H, 2 * H, 0, W).row(y);
            Mat x2Data = geometry.submat(2 * H, 3 * H, 0, W).row(y);
            Mat x3Data = geometry.submat(3 * H, 4 * H, 0, W).row(y);
            Mat anglesData = geometry.submat(4 * H, 5 * H, 0, W).row(y);

            for (int x = 0; x < W; ++x) {
                double score = scoresData.get(0, x)[0];
//                LOGGER.d("score : "+(int)score+", y : "+y+", x : "+x);

                if (score >= scoreThresh) {
                    //LOGGER.d("index : "+x+", score : "+score);

                    double offsetX = x * 4.0;
                    double offsetY = y * 4.0;
                    double angle = anglesData.get(0, x)[0];
                    double cosA = cos(angle);
                    double sinA = sin(angle);
                    double x0 = x0Data.get(0, x)[0];
                    double x1 = x1Data.get(0, x)[0];
                    double x2 = x2Data.get(0, x)[0];
                    double x3 = x3Data.get(0, x)[0];

                    double h = x0 + x2;
                    double w = x1 + x3;
                    Point offset = new Point(offsetX + cosA * x1 + sinA * x2, offsetY - sinA * x1 + cosA * x2);
                    Point p1 = new Point(-1 * sinA * h + offset.x, -1 * cosA * h + offset.y);
                    Point p3 = new Point(-1 * cosA * w + offset.x,      sinA * w + offset.y); // original trouble here !
                    RotatedRect r = new RotatedRect(new Point(0.5 * (p1.x + p3.x), 0.5 * (p1.y + p3.y)), new Size(w, h), -1 * angle * 180 / Math.PI);

                    detections.add(r);
                    confidences.add((float) score);
                }
            }
        }
        return detections;
    }

    //sally : 4점을 rotate하는 함수
    private static Point[] rotateRectPts(Point[] in_pts, double rad, Point center) {
        Point[] rotated = new Point[4];
        for (int i = 0; i < 4; i++) {
            double rotatedX = (in_pts[i].x - center.x)*cos(rad) - (in_pts[i].y - center.y)*sin(rad) + center.x;
            double rotatedY = (in_pts[i].x - center.x)*sin(rad) + (in_pts[i].y - center.y)*cos(rad) + center.y;

            rotated[i] = new Point(rotatedX, rotatedY);
        }
        return rotated;
    }

    private static double distance(Point p1, Point p2){ //sally
        double distance;

        // 피타고라스의 정리
        // pow(x,2) x의 2승,  sqrt() 제곱근
        distance = sqrt(pow(p1.x - p2.x, 2) + pow(p1.y - p2.y, 2));

        return distance;
    }

    //rect 를 받아 확장된 점을 리턴하는 함수(andgle적용)
    public static Point[] expandedRotatedRect(RotatedRect rotRect, float expandRatioX, float expandRatioY) {
        Point[] rotRectPts = new Point[4];
        double angle = rotRect.angle;
        double rad = (double) angle * 3.1416 / 180;
        Point center = new Point(rotRect.center.x, rotRect.center.y);
        float expandX = expandRatioX; //0.1f;
        float expandY = expandRatioY; //0.1f;

        //1. rotatedRect의 center를 중심으로 -angle만큼 회전하기
        rotRect.points(rotRectPts);
        Point[] rectPts = rotateRectPts(rotRectPts, -rad, center);
        //2. 1의 결과에서 확장된 점 구하기, 가로,세로 길이의 0.1만큼을 확장한다.
        double horizontal = distance(rectPts[1], rectPts[2]); //width
        double vertical = distance(rectPts[0], rectPts[1]); //height
        double offsetX = horizontal * expandX;
        double offsetY = vertical * expandY;
        rectPts[0].x = rectPts[0].x - offsetX; //좌
        rectPts[1].x = rectPts[1].x - offsetX; //좌
        rectPts[2].x = rectPts[2].x + offsetX; //우
        rectPts[3].x = rectPts[3].x + offsetX; //우
        rectPts[0].y = rectPts[0].y + offsetY; //하
        rectPts[1].y = rectPts[1].y - offsetY; //상
        rectPts[2].y = rectPts[2].y - offsetY; //상
        rectPts[3].y = rectPts[3].y + offsetY; //하

        //3. 확장된 점을 다시 angle만큼 회전하기
        Point[] expandedRotatedRectPts = rotateRectPts(rectPts, rad, center);

        return expandedRotatedRectPts;
    }

    public static class DetectedRects implements Comparable<DetectedRects> {
        private int mIndex;
        private RotatedRect mRotRect;
        private double mArea;
        public DetectedRects(int index, RotatedRect rect) {
            mIndex = index;
            mRotRect = rect;
            mArea = rect.size.width * rect.size.height;
        }
        public int getIdx() { return mIndex; }
        public RotatedRect getRotRect() { return mRotRect; }

        @Override
        public int compareTo(DetectedRects o) {
            double targetArea = o.getRotRect().size.width * o.getRotRect().size.height;
            if(mArea == targetArea) return 0;
            else if(mArea > targetArea) return 1;
            else                    return -1;

        }
    }
}
